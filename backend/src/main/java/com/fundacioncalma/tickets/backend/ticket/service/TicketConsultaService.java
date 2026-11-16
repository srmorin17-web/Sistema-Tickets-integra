package com.fundacioncalma.tickets.backend.ticket.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fundacioncalma.tickets.backend.rrhh.dto.EmpleadoDTO;
import com.fundacioncalma.tickets.backend.rrhh.service.RrhhClientService;
import com.fundacioncalma.tickets.backend.ticket.dto.TicketDetalleDTO;
import com.fundacioncalma.tickets.backend.ticket.dto.TicketDetalleDTO.HistorialItem;
import com.fundacioncalma.tickets.backend.ticket.model.Estado;
import com.fundacioncalma.tickets.backend.ticket.model.HistorialAuditoria;
import com.fundacioncalma.tickets.backend.ticket.model.Ticket;
import com.fundacioncalma.tickets.backend.ticket.repository.EstadoRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.HistorialAuditoriaRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

/**
 * Consultas de solo lectura sobre tickets: listado del solicitante y detalle con historial.
 */
@Service
@RequiredArgsConstructor
public class TicketConsultaService {

    /** Roles que pueden consultar cualquier ticket. El resto solo ve los propios. */
    private static final Set<String> ROLES_PERSONAL = Set.of(
            "ROLE_Técnico TI", "ROLE_Supervisor", "ROLE_Administrador", "ROLE_Superadmin");

    private final TicketRepository ticketRepository;
    private final HistorialAuditoriaRepository historialRepository;
    private final EstadoRepository estadoRepository;
    private final RrhhClientService rrhhClientService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<TicketDetalleDTO> misTickets(String correo) {
        return ticketRepository.findBySolicitanteCorreo(correo).stream()
                .map(t -> aDto(t, null, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketDetalleDTO> todosLosTickets() {
        return ticketRepository.findAllByOrderByFechaCreacionDesc().stream()
                .map(t -> aDto(t, null, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketDetalleDTO detalle(UUID id, Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));

        boolean esPersonal = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLES_PERSONAL::contains);
        boolean esPropio = ticket.getSolicitante().getCorreoCorporativo()
                .equalsIgnoreCase(authentication.getName());
        boolean esRepresentante = esRepresentanteDelArea(ticket, authentication.getName());

        if (!esPersonal && !esPropio && !esRepresentante) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a este ticket");
        }

        Map<UUID, String> nombresEstado = new HashMap<>();
        for (Estado e : estadoRepository.findAll()) {
            nombresEstado.put(e.getId(), e.getNombre());
        }

        // El personal de TI y el representante del área escalada ven el detalle completo; el solicitante, la vista reducida
        boolean verInterno = esPersonal || esRepresentante;

        List<HistorialItem> historial = historialRepository.findByTicketIdOrdenado(id).stream()
                .filter(h -> verInterno || !"REASIGNACION".equals(h.getTipoAccion()))
                .map(h -> aHistorialItem(h, nombresEstado))
                .toList();

        return aDto(ticket, historial, !verInterno);
    }

    /** Con ocultarInterno (vista del solicitante) se omite el área y el motivo de escalamiento. */
    private TicketDetalleDTO aDto(Ticket t, List<HistorialItem> historial, boolean ocultarInterno) {
        String tecnico = t.getTecnicoAsignado() == null || t.getTecnicoAsignado().getEmpleado() == null
                ? null
                : t.getTecnicoAsignado().getEmpleado().getNombres() + " "
                        + t.getTecnicoAsignado().getEmpleado().getApellidos();

        return new TicketDetalleDTO(
                t.getId(),
                t.getCodigo(),
                t.getAsunto(),
                t.getDescripcion(),
                t.getSolicitante().getNombres() + " " + t.getSolicitante().getApellidos(),
                t.getSolicitante().getCorreoCorporativo(),
                tecnico,
                t.getCategoria().getNombre(),
                t.getPrioridad().getNombre(),
                t.getEstado().getId(),
                t.getEstado().getNombre(),
                !ocultarInterno && t.getAreaEscalada() != null ? t.getAreaEscalada().getNombre() : null,
                !ocultarInterno ? representanteDe(t) : null,
                ocultarInterno ? null : t.getMotivoEscalamiento(),
                t.getFechaCreacion(),
                t.getFechaActualizacion(),
                t.getFechaVencimientoSla(),
                t.getSlaHorasSnapshot(),
                Boolean.TRUE.equals(t.getPausado()),
                t.getFechaCierre(),
                t.getComentarioCierre(),
                historial == null || !ocultarInterno ? historial : historial.stream().map(this::sinDatosInternos).toList());
    }

    private boolean esRepresentanteDelArea(Ticket t, String correo) {
        if (t.getAreaEscalada() == null || t.getAreaEscalada().getContactoResponsableId() == null) {
            return false;
        }
        return rrhhClientService.obtenerEmpleadoPorCorreo(correo)
                .map(e -> e.getId().equals(t.getAreaEscalada().getContactoResponsableId()))
                .orElse(false);
    }

    private String representanteDe(Ticket t) {
        if (t.getAreaEscalada() == null || t.getAreaEscalada().getContactoResponsableId() == null) {
            return null;
        }
        return rrhhClientService.obtenerResumen(t.getAreaEscalada().getContactoResponsableId())
                .map(EmpleadoDTO::nombre).orElse(null);
    }

    /** Quién ejecutó la acción; el cierre automático no tiene usuario y se muestra como "Sistema". */
    private String actor(HistorialAuditoria h, String estadoNuevo) {
        if (h.getUsuario() != null && h.getUsuario().getEmpleado() != null) {
            return h.getUsuario().getEmpleado().getNombres() + " " + h.getUsuario().getEmpleado().getApellidos();
        }
        return "Cerrado".equals(estadoNuevo) ? "Sistema" : null;
    }

    /** Vista del solicitante: sin el nombre de quien actuó ni los datos internos del escalamiento. */
    private HistorialItem sinDatosInternos(HistorialItem h) {
        boolean escalamiento = "ESCALAMIENTO".equals(h.tipoAccion());
        return new HistorialItem(h.tipoAccion(), h.estadoAnterior(), h.estadoNuevo(),
                escalamiento ? null : h.comentario(), escalamiento ? null : h.area(), null, null, h.fecha());
    }

    private HistorialItem aHistorialItem(HistorialAuditoria h, Map<UUID, String> nombresEstado) {
        JsonNode datos = leerJson(h.getDatosNuevos());
        // CAMBIO_ESTADO guarda "comentario"; ESCALAMIENTO guarda "motivo" y "area".
        String comentario = texto(datos, "comentario");
        if (comentario == null) {
            comentario = texto(datos, "motivo");
        }
        if (comentario == null) {
            comentario = texto(datos, "archivo"); // eventos de adjuntos
        }
        return new HistorialItem(
                h.getTipoAccion(),
                h.getEstadoAnteriorId() != null ? nombresEstado.get(h.getEstadoAnteriorId()) : null,
                h.getEstadoNuevoId() != null ? nombresEstado.get(h.getEstadoNuevoId()) : null,
                comentario,
                texto(datos, "area"),
                texto(datos, "tecnico"),
                actor(h, h.getEstadoNuevoId() != null ? nombresEstado.get(h.getEstadoNuevoId()) : null),
                h.getTimestamp());
    }

    /** Los campos jsonb se guardan como String; según el driver pueden venir con doble codificación. */
    private JsonNode leerJson(String crudo) {
        if (crudo == null || crudo.isBlank()) {
            return null;
        }
        try {
            JsonNode nodo = objectMapper.readTree(crudo);
            if (nodo != null && nodo.isTextual()) {
                nodo = objectMapper.readTree(nodo.asText());
            }
            return nodo;
        } catch (Exception e) {
            return null;
        }
    }

    private String texto(JsonNode nodo, String campo) {
        if (nodo == null) {
            return null;
        }
        JsonNode valor = nodo.get(campo);
        return (valor == null || valor.isNull()) ? null : valor.asText();
    }
}
