package com.fundacioncalma.tickets.backend.rrhh.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fundacioncalma.tickets.backend.rrhh.cliente.CacheTemporal;
import com.fundacioncalma.tickets.backend.rrhh.cliente.EmpleadoInactivoException;
import com.fundacioncalma.tickets.backend.rrhh.cliente.EmpleadoRrhh;
import com.fundacioncalma.tickets.backend.rrhh.cliente.RrhhNoDisponibleException;
import com.fundacioncalma.tickets.backend.rrhh.cliente.RrhhRestCliente;
import com.fundacioncalma.tickets.backend.rrhh.dto.EmpleadoDTO;
import com.fundacioncalma.tickets.backend.rrhh.model.Empleado;
import com.fundacioncalma.tickets.backend.rrhh.repository.EmpleadoRepository;
import com.fundacioncalma.tickets.backend.rrhh.repository.EmpleadoRepository.EmpleadoResumen;

import lombok.extern.slf4j.Slf4j;

/**
 * Único punto de acceso a los datos de RR. HH. (empleados y jerarquía). El resto del sistema no conoce de dónde vienen.
 *
 * rrhh.proveedor=local (por defecto): se lee la tabla empleado.empleado.
 * rrhh.proveedor=rest: se consulta la API de RR. HH. (contrato v1) y se mantiene la réplica local.
 *   - sincronizarSolicitante / sincronizarEmpleadoPorId: consultan a RR. HH., exigen que el empleado esté activo y lo
 *     guardan localmente (las claves foráneas lo requieren). Si RR. HH. no responde, se usa la copia local.
 *   - obtenerResumen / obtenerCorreoLider / buscarEmpleados: consultan a RR. HH. (con caché corto) y, si no responde,
 *     la copia local.
 *   - obtenerEmpleadoPorCorreo: SOLO la copia local. Se usa para identificar al usuario ya autenticado en cada
 *     petición; no debe llamar a RR. HH. cada vez.
 */
@Slf4j
@Service
public class RrhhClientService {

    private static final int LIMITE_MAXIMO = 50;

    private final EmpleadoRepository empleadoRepository;
    private final RrhhSincronizador sincronizador;
    private final RrhhRestCliente rest; // nulo con rrhh.proveedor=local
    private final String estadoActivo;
    private final CacheTemporal<UUID, EmpleadoRrhh> cache;

    public RrhhClientService(
            EmpleadoRepository empleadoRepository,
            RrhhSincronizador sincronizador,
            ObjectProvider<RrhhRestCliente> restProvider,
            @Value("${rrhh.estado-activo:ACTIVO}") String estadoActivo,
            @Value("${rrhh.cache-segundos:300}") long cacheSegundos) {
        this.empleadoRepository = empleadoRepository;
        this.sincronizador = sincronizador;
        this.rest = restProvider.getIfAvailable();
        this.estadoActivo = estadoActivo;
        this.cache = new CacheTemporal<>(Duration.ofSeconds(cacheSegundos));
    }

    /** Copia local del empleado. No consulta a RR. HH. */
    public Optional<Empleado> obtenerEmpleadoPorCorreo(String correo) {
        return empleadoRepository.findByCorreoCorporativo(correo);
    }

    /**
     * Valida al solicitante de un ticket contra RR. HH.: debe existir y estar activo. Devuelve la copia local, ya actualizada.
     * @throws EmpleadoInactivoException si RR. HH. lo informa como no activo.
     */
    public Optional<Empleado> sincronizarSolicitante(String correo) {
        if (rest == null) {
            return obtenerEmpleadoPorCorreo(correo);
        }
        try {
            Optional<EmpleadoRrhh> remoto = rest.porCorreo(correo);
            if (remoto.isEmpty()) {
                return Optional.empty(); // RR. HH. es la fuente de verdad: si no lo conoce, no es un solicitante válido
            }
            EmpleadoRrhh empleado = remoto.get();
            if (!esActivo(empleado)) {
                throw new EmpleadoInactivoException(correo);
            }
            sincronizador.guardar(empleado);
            cache.guardar(empleado.id(), empleado);
            return empleadoRepository.findById(empleado.id());
        } catch (RrhhNoDisponibleException e) {
            log.warn("⚠️ RR. HH. no disponible ({}). Se usa la copia local de {}", e.getMessage(), correo);
            return obtenerEmpleadoPorCorreo(correo);
        }
    }

    /** Resuelve y deja en la réplica local a un empleado elegido por su id (p. ej. el representante de un área). */
    public Optional<EmpleadoDTO> sincronizarEmpleadoPorId(UUID id) {
        if (rest == null) {
            return resumenLocal(id);
        }
        try {
            Optional<EmpleadoRrhh> remoto = rest.porId(id);
            if (remoto.isEmpty()) {
                return Optional.empty();
            }
            EmpleadoRrhh empleado = remoto.get();
            if (!esActivo(empleado)) {
                throw new EmpleadoInactivoException(empleado.correoCorporativo());
            }
            sincronizador.guardar(empleado);
            cache.guardar(id, empleado);
            return Optional.of(aDto(empleado));
        } catch (RrhhNoDisponibleException e) {
            log.warn("⚠️ RR. HH. no disponible ({}). Se usa la copia local del empleado {}", e.getMessage(), id);
            return resumenLocal(id);
        }
    }

    /** Datos para mostrar de un empleado (nombre, correo, cargo). */
    public Optional<EmpleadoDTO> obtenerResumen(UUID empleadoId) {
        if (rest == null) {
            return resumenLocal(empleadoId);
        }
        Optional<EmpleadoRrhh> enCache = cache.obtener(empleadoId);
        if (enCache.isPresent()) {
            return enCache.map(this::aDto);
        }
        try {
            Optional<EmpleadoRrhh> remoto = rest.porId(empleadoId);
            remoto.ifPresent(e -> cache.guardar(empleadoId, e));
            return remoto.map(this::aDto); // vacío = RR. HH. no conoce al empleado
        } catch (RrhhNoDisponibleException e) {
            log.warn("⚠️ RR. HH. no disponible ({}). Se usa la copia local del empleado {}", e.getMessage(), empleadoId);
            return resumenLocal(empleadoId);
        }
    }

    /** Correo del jefe directo del empleado, para la copia en tickets de prioridad Crítica. */
    public Optional<String> obtenerCorreoLider(UUID empleadoId) {
        if (rest == null) {
            return empleadoRepository.findCorreoLiderByEmpleadoId(empleadoId).filter(c -> !c.isBlank());
        }
        try {
            Optional<EmpleadoRrhh> remoto = cache.obtener(empleadoId).or(() -> rest.porId(empleadoId));
            remoto.ifPresent(e -> cache.guardar(empleadoId, e));
            return remoto.map(EmpleadoRrhh::liderCorreo).filter(c -> c != null && !c.isBlank());
        } catch (RrhhNoDisponibleException e) {
            log.warn("⚠️ RR. HH. no disponible ({}). Se usa la jerarquía local", e.getMessage());
            return empleadoRepository.findCorreoLiderByEmpleadoId(empleadoId).filter(c -> !c.isBlank());
        }
    }

    /** Empleados activos cuyo nombre, apellido o correo contiene el texto (para elegir al representante de un área). */
    public List<EmpleadoDTO> buscarEmpleados(String texto, Integer limite) {
        int tope = (limite == null || limite < 1) ? 20 : Math.min(limite, LIMITE_MAXIMO);
        String criterio = texto == null ? "" : texto.trim();
        if (rest != null) {
            try {
                return rest.buscar(criterio, tope).stream().filter(this::esActivo).map(this::aDto).toList();
            } catch (RrhhNoDisponibleException e) {
                log.warn("⚠️ RR. HH. no disponible ({}). La búsqueda usa la copia local", e.getMessage());
            }
        }
        return empleadoRepository.buscar(criterio, tope).stream().map(this::aDto).toList();
    }

    // ----- auxiliares -----

    private Optional<EmpleadoDTO> resumenLocal(UUID id) {
        return empleadoRepository.findResumenById(id).map(this::aDto);
    }

    private boolean esActivo(EmpleadoRrhh e) {
        // Un estado ausente se considera activo (el contrato lo exige; no se bloquea a nadie por un dato faltante)
        return e.estadoLaboral() == null || e.estadoLaboral().isBlank() || estadoActivo.equalsIgnoreCase(e.estadoLaboral().trim());
    }

    private EmpleadoDTO aDto(EmpleadoRrhh e) {
        return new EmpleadoDTO(e.id(), e.nombres() + " " + e.apellidos(), e.correoCorporativo(), e.cargo());
    }

    private EmpleadoDTO aDto(EmpleadoResumen e) {
        return new EmpleadoDTO(e.getId(), e.getNombres() + " " + e.getApellidos(), e.getCorreoCorporativo(), e.getCargo());
    }
}
