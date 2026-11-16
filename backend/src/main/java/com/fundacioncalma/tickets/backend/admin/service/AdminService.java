package com.fundacioncalma.tickets.backend.admin.service;

import com.fundacioncalma.tickets.backend.admin.dto.*;
import com.fundacioncalma.tickets.backend.admin.dto.AdminCatalogosResponse.AreaAdmin;
import com.fundacioncalma.tickets.backend.admin.dto.AdminCatalogosResponse.CategoriaAdmin;
import com.fundacioncalma.tickets.backend.admin.dto.AdminCatalogosResponse.PrioridadAdmin;
import com.fundacioncalma.tickets.backend.admin.dto.AdminCatalogosResponse.Representante;
import com.fundacioncalma.tickets.backend.rrhh.cliente.EmpleadoInactivoException;
import com.fundacioncalma.tickets.backend.rrhh.service.RrhhClientService;
import com.fundacioncalma.tickets.backend.ticket.model.*;
import com.fundacioncalma.tickets.backend.ticket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final CategoriaRepository categoriaRepository;
    private final PrioridadRepository prioridadRepository;
    private final AreaEscalamientoRepository areaEscalamientoRepository;
    private final TicketRepository ticketRepository;
    private final RrhhClientService rrhhClientService;

    // ==========================================
    // CONSULTA COMPLETA (incluye inactivos)
    // ==========================================
    @Transactional(readOnly = true)
    public AdminCatalogosResponse obtenerCatalogos() {
        var categorias = categoriaRepository.findAll().stream()
                .sorted(Comparator.comparing(Categoria::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(this::aCategoria)
                .toList();
        var prioridades = prioridadRepository.findAll().stream()
                .sorted(Comparator.comparing(Prioridad::getSlaHoras))
                .map(this::aPrioridad)
                .toList();
        var areas = areaEscalamientoRepository.findAll().stream()
                .sorted(Comparator.comparing(AreaEscalamiento::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(this::aArea)
                .toList();
        return new AdminCatalogosResponse(categorias, prioridades, areas);
    }

    // ==========================================
    // GESTIÓN DE CATEGORÍAS (RF-31, RF-33)
    // ==========================================
    @Transactional
    public CategoriaAdmin crearCategoria(CategoriaRequest request) {
        String nombre = texto(request.getNombre(), 100, "El nombre");
        if (categoriaRepository.existsByNombre(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre");
        }
        Prioridad prioridad = prioridadRequerida(request.getPrioridadDefectoId());

        Categoria categoria = Categoria.builder()
                .nombre(nombre)
                .descripcion(opcional(request.getDescripcion(), 500))
                .activo(request.getActivo() == null || request.getActivo())
                .prioridadDefecto(prioridad)
                .build();

        log.info("⚙️ Categoría creada: {}", nombre);
        return aCategoria(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaAdmin actualizarCategoria(UUID id, CategoriaRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));

        String nombre = texto(request.getNombre(), 100, "El nombre");
        // Validar nombre duplicado si se está cambiando
        if (!categoria.getNombre().equals(nombre) && categoriaRepository.existsByNombre(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre");
        }

        categoria.setNombre(nombre);
        categoria.setDescripcion(opcional(request.getDescripcion(), 500));
        if (request.getActivo() != null) {
            categoria.setActivo(request.getActivo()); // Desactivar no borra el historial (RF-33)
        }
        if (request.getPrioridadDefectoId() != null) {
            categoria.setPrioridadDefecto(prioridadRequerida(request.getPrioridadDefectoId()));
        }

        log.info("⚙️ Categoría actualizada: {}", categoria.getNombre());
        return aCategoria(categoriaRepository.save(categoria));
    }

    // ==========================================
    // GESTIÓN DE PRIORIDADES Y SLA (RF-32, RF-33)
    // ==========================================
    @Transactional
    public PrioridadAdmin actualizarPrioridad(UUID id, PrioridadRequest request) {
        if (request.getSlaHoras() == null || request.getSlaHoras() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tiempo de SLA debe ser mayor a cero");
        }

        Prioridad prioridad = prioridadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prioridad no encontrada"));

        prioridad.setNombre(texto(request.getNombre(), 50, "El nombre"));
        prioridad.setSlaHoras(request.getSlaHoras()); // Solo afectará a los NUEVOS tickets (RF-33)

        log.info("⚙️ Prioridad actualizada: {} -> {} horas", prioridad.getNombre(), prioridad.getSlaHoras());
        return aPrioridad(prioridadRepository.save(prioridad));
    }

    // ==========================================
    // GESTIÓN DE ÁREAS DE ESCALAMIENTO (RF-34, RF-35)
    // ==========================================
    @Transactional
    public AreaAdmin crearArea(AreaEscalamientoRequest request) {
        String nombre = texto(request.getNombre(), 150, "El nombre");
        if (areaEscalamientoRepository.existsByNombre(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un área con ese nombre");
        }

        validarRepresentante(request.getContactoResponsableId());

        AreaEscalamiento area = AreaEscalamiento.builder()
                .nombre(nombre)
                .descripcion(opcional(request.getDescripcion(), 500))
                .contactoResponsableId(request.getContactoResponsableId())
                .activo(request.getActivo() == null || request.getActivo())
                .build();

        log.info("⚙️ Área de escalamiento creada: {}", nombre);
        return aArea(areaEscalamientoRepository.save(area));
    }

    @Transactional
    public AreaAdmin actualizarArea(UUID id, AreaEscalamientoRequest request) {
        AreaEscalamiento area = areaEscalamientoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Área no encontrada"));

        String nombre = texto(request.getNombre(), 150, "El nombre");
        if (!area.getNombre().equals(nombre) && areaEscalamientoRepository.existsByNombre(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un área con ese nombre");
        }

        // RF-35: Bloquear desactivación si hay tickets activos
        if (request.getActivo() != null && !request.getActivo() && area.getActivo()) {
            long ticketsActivos = ticketRepository.countTicketsActivosPorArea(id);
            if (ticketsActivos > 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "RF-35: Existen " + ticketsActivos + " tickets activos asignados a esta área. Resuélvalos antes de desactivarla.");
            }
        }

        validarRepresentante(request.getContactoResponsableId());

        area.setNombre(nombre);
        area.setDescripcion(opcional(request.getDescripcion(), 500));
        area.setContactoResponsableId(request.getContactoResponsableId()); // nulo = el área queda sin representante
        if (request.getActivo() != null) area.setActivo(request.getActivo());

        log.info("⚙️ Área de escalamiento actualizada: {}", area.getNombre());
        return aArea(areaEscalamientoRepository.save(area));
    }

    // ==========================================
    // AUXILIARES
    // ==========================================
    private Prioridad prioridadRequerida(UUID id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La prioridad por defecto es obligatoria");
        }
        return prioridadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La prioridad indicada no existe"));
    }

    private String texto(String valor, int max, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " es obligatorio");
        }
        String limpio = valor.trim();
        if (limpio.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " no puede superar los " + max + " caracteres");
        }
        return limpio;
    }

    private String opcional(String valor, int max) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        if (limpio.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción no puede superar los " + max + " caracteres");
        }
        return limpio;
    }

    // Se devuelven DTO (no entidades): Categoria.prioridadDefecto es LAZY y no se puede serializar fuera de la transacción
    private CategoriaAdmin aCategoria(Categoria c) {
        Prioridad p = c.getPrioridadDefecto();
        return new CategoriaAdmin(c.getId(), c.getNombre(), c.getDescripcion(), Boolean.TRUE.equals(c.getActivo()),
                p != null ? p.getId() : null, p != null ? p.getNombre() : null);
    }

    private PrioridadAdmin aPrioridad(Prioridad p) {
        return new PrioridadAdmin(p.getId(), p.getNombre(), p.getSlaHoras());
    }

    private AreaAdmin aArea(AreaEscalamiento a) {
        Representante representante = a.getContactoResponsableId() == null ? null
                : rrhhClientService.obtenerResumen(a.getContactoResponsableId())
                        .map(e -> new Representante(e.id(), e.nombre(), e.correo(), e.cargo()))
                        .orElse(null);
        return new AreaAdmin(a.getId(), a.getNombre(), a.getDescripcion(), Boolean.TRUE.equals(a.getActivo()), representante);
    }

    /** El representante debe existir en RR. HH. (es opcional: un área puede crearse y asignarse después). */
    private void validarRepresentante(UUID empleadoId) {
        if (empleadoId == null) {
            return;
        }
        try {
            // Además de validar, deja al empleado en la réplica local (la clave foránea del área lo exige)
            if (rrhhClientService.sincronizarEmpleadoPorId(empleadoId).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El representante indicado no existe en RR. HH.");
            }
        } catch (EmpleadoInactivoException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El representante indicado no está activo en RR. HH.");
        }
    }
}
