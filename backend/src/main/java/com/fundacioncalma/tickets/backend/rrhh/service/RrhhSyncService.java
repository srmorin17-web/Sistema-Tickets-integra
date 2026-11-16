package com.fundacioncalma.tickets.backend.rrhh.service;

import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.fundacioncalma.tickets.backend.rrhh.cliente.EmpleadoRrhh;
import com.fundacioncalma.tickets.backend.rrhh.cliente.RrhhNoDisponibleException;
import com.fundacioncalma.tickets.backend.rrhh.cliente.RrhhCambioEmpleado;
import com.fundacioncalma.tickets.backend.rrhh.cliente.RrhhCambiosPage;
import com.fundacioncalma.tickets.backend.rrhh.cliente.RrhhRestCliente;
import com.fundacioncalma.tickets.backend.rrhh.cliente.RrhhSnapshotPage;
import com.fundacioncalma.tickets.backend.rrhh.dto.EstadoSincronizacionRrhhDTO;
import com.fundacioncalma.tickets.backend.usuario.service.ProvisionamientoCuentasService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RrhhSyncService {

    private static final int LIMITE_API = 500;
    private final ObjectProvider<RrhhRestCliente> clienteProvider;
    private final RrhhSyncStateStore stateStore;
    private final RrhhSincronizador sincronizador;
    private final ProvisionamientoCuentasService provisionamientoCuentasService;
    private final TransactionTemplate transaccion;
    private final int tamanoPagina;
    private final long intervaloMinutos;

    public RrhhSyncService(
            ObjectProvider<RrhhRestCliente> clienteProvider,
            RrhhSyncStateStore stateStore,
            RrhhSincronizador sincronizador,
            ProvisionamientoCuentasService provisionamientoCuentasService,
            PlatformTransactionManager transactionManager,
            @Value("${rrhh.sync.tamano-pagina:500}") int tamanoPagina,
            @Value("${rrhh.sync.intervalo-ms:600000}") long intervaloMs) {
        this.clienteProvider = clienteProvider;
        this.stateStore = stateStore;
        this.sincronizador = sincronizador;
        this.provisionamientoCuentasService = provisionamientoCuentasService;
        this.transaccion = new TransactionTemplate(transactionManager);
        this.tamanoPagina = Math.max(1, Math.min(tamanoPagina, LIMITE_API));
        this.intervaloMinutos = Math.max(1, intervaloMs / 60_000);
    }

    @Scheduled(
            fixedDelayString = "${rrhh.sync.intervalo-ms:600000}",
            initialDelayString = "${rrhh.sync.demora-inicial-ms:15000}")
    public synchronized void sincronizarProgramado() {
        RrhhRestCliente cliente = clienteProvider.getIfAvailable();
        if (cliente == null) {
            return;
        }

        stateStore.iniciarIntento();
        ejecutarSincronizacion(cliente);
    }

    public synchronized EstadoSincronizacionRrhhDTO sincronizarAhora() {
        RrhhRestCliente cliente = clienteProvider.getIfAvailable();
        if (cliente == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "La integración con RR. HH. no está habilitada.");
        }
        if (!stateStore.reservarSincronizacionManual()) {
            if (stateStore.cooldownManualSegundos() > 0) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS, "Espere antes de volver a sincronizar con RR. HH.");
            }
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Ya hay una sincronización de RR. HH. en curso.");
        }

        String error = ejecutarSincronizacion(cliente);
        if (error != null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, error);
        }
        return obtenerEstado();
    }

    private String ejecutarSincronizacion(RrhhRestCliente cliente) {
        try {
            sincronizarSnapshot(cliente);
            sincronizarCambios(cliente);
            stateStore.marcarConectada();
            return null;
        } catch (RuntimeException e) {
            String mensaje = mensajeError(e);
            log.error("Falló la sincronización de empleados desde RR. HH.: {}", mensaje, e);
            stateStore.marcarError(mensaje);
            return mensaje;
        }
    }

    private String mensajeError(RuntimeException e) {
        return e instanceof RrhhNoDisponibleException || e instanceof IllegalStateException
                ? Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName())
                : "No se pudo aplicar la sincronización en la base de datos local.";
    }

    public EstadoSincronizacionRrhhDTO obtenerEstado() {
        return stateStore.estado(clienteProvider.getIfAvailable() != null, intervaloMinutos);
    }

    private void sincronizarSnapshot(RrhhRestCliente cliente) {
        RrhhSyncStateStore.SyncState estado = stateStore.obtener();
        while (!estado.snapshotCompleto()) {
            RrhhSnapshotPage pagina = cliente.snapshot(
                    estado.snapshotAfterId(),
                    estado.snapshotVersion(),
                    tamanoPagina);
            stateStore.registrarConexion();
            if (pagina.empleados() == null || pagina.cursorCambios() < 0) {
                throw new IllegalStateException("RR. HH. devolvió un snapshot incompleto");
            }
            if (estado.snapshotVersion() != null && estado.snapshotVersion() != pagina.cursorCambios()) {
                throw new IllegalStateException("RR. HH. cambió el cursor durante la paginación del snapshot");
            }
            if (pagina.hayMas()
                    && (pagina.siguienteId() == null || pagina.siguienteId().equals(estado.snapshotAfterId()))) {
                throw new IllegalStateException("RR. HH. devolvió una página de snapshot sin cursor de continuación");
            }
            Boolean completa = transaccion.execute(status -> {
                for (EmpleadoRrhh empleado : pagina.empleados()) {
                    if (empleado == null) {
                        throw new IllegalStateException("RR. HH. devolvió un empleado vacío en el snapshot");
                    }
                    sincronizador.guardar(empleado);
                    provisionamientoCuentasService.encolarSiAutomatico(empleado.id());
                }
                boolean terminada = !pagina.hayMas();
                stateStore.guardarPaginaSnapshot(
                        pagina.siguienteId(),
                        pagina.cursorCambios(),
                        terminada,
                        pagina.empleados().size());
                return terminada;
            });
            estado = stateStore.obtener();
            if (Boolean.TRUE.equals(completa)) {
                break;
            }
        }
    }

    private void sincronizarCambios(RrhhRestCliente cliente) {
        while (true) {
            long cursorActual = stateStore.obtener().cursorVersion();
            RrhhCambiosPage pagina = cliente.cambios(cursorActual, tamanoPagina);
            stateStore.registrarConexion();
            if (pagina.cambios() == null) {
                throw new IllegalStateException("RR. HH. devolvió una página de cambios incompleta");
            }
            if (pagina.cursor() < cursorActual) {
                throw new IllegalStateException("RR. HH. devolvió un cursor de cambios anterior al guardado");
            }
            if (pagina.hayMas() && (pagina.cambios().isEmpty() || pagina.cursor() == cursorActual)) {
                throw new IllegalStateException("RR. HH. devolvió una página de cambios sin avanzar el cursor");
            }
            transaccion.executeWithoutResult(status -> {
                long versionAnterior = cursorActual;
                for (RrhhCambioEmpleado cambio : pagina.cambios()) {
                    if (cambio == null) {
                        throw new IllegalStateException("RR. HH. devolvió un cambio vacío");
                    }
                    if (cambio.version() <= versionAnterior) {
                        throw new IllegalStateException("RR. HH. devolvió cambios fuera de orden");
                    }
                    aplicar(cambio);
                    versionAnterior = cambio.version();
                }
                if (pagina.cursor() < versionAnterior) {
                    throw new IllegalStateException("RR. HH. devolvió un cursor anterior al último cambio");
                }
                stateStore.guardarCursorCambios(pagina.cursor(), pagina.cambios().size());
            });
            if (!pagina.hayMas()) {
                return;
            }
        }
    }

    private void aplicar(RrhhCambioEmpleado cambio) {
        if (cambio.tipo() == null) {
            throw new IllegalStateException("RR. HH. devolvió un cambio sin tipo");
        }
        switch (cambio.tipo()) {
            case "UPSERT" -> {
                if (cambio.empleado() == null) {
                    throw new IllegalStateException("RR. HH. devolvió un UPSERT sin empleado");
                }
                sincronizador.guardar(cambio.empleado());
                provisionamientoCuentasService.encolarSiAutomatico(cambio.empleado().id());
            }
            case "DELETE" -> {
                if (cambio.idEmpleado() == null) {
                    throw new IllegalStateException("RR. HH. devolvió un DELETE sin idEmpleado");
                }
                sincronizador.desactivar(cambio.idEmpleado());
            }
            default -> throw new IllegalStateException("RR. HH. devolvió un tipo de cambio no reconocido");
        }
    }
}
