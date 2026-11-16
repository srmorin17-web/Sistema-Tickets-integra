package com.fundacioncalma.tickets.backend.ticket.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fundacioncalma.tickets.backend.rrhh.model.Empleado;
import com.fundacioncalma.tickets.backend.rrhh.service.RrhhClientService;
import com.fundacioncalma.tickets.backend.ticket.dto.AreaRepresentanteDTO;
import com.fundacioncalma.tickets.backend.ticket.dto.CambiarEstadoRequest;
import com.fundacioncalma.tickets.backend.ticket.event.KafkaProducerService;
import com.fundacioncalma.tickets.backend.ticket.model.AreaEscalamiento;
import com.fundacioncalma.tickets.backend.ticket.model.Estado;
import com.fundacioncalma.tickets.backend.ticket.model.Ticket;
import com.fundacioncalma.tickets.backend.ticket.repository.AreaEscalamientoRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.EstadoRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.TicketRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lado del representante de un área de escalamiento. El acceso no depende de un rol: lo tiene quien es
 * el representante (contacto_responsable_id, un empleado de RR. HH.) del área a la que se escaló el ticket.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EscalamientoAreaService {

    private static final int MAX_COMENTARIO = 1000;

    private final RrhhClientService rrhhClientService;
    private final AreaEscalamientoRepository areaRepository;
    private final TicketRepository ticketRepository;
    private final EstadoRepository estadoRepository;
    private final TicketService ticketService;
    private final KafkaProducerService kafkaProducerService;

    /** Áreas que representa el usuario; vacío si no es representante de ninguna. */
    @Transactional(readOnly = true)
    public List<AreaRepresentanteDTO.Area> misAreas(String correo) {
        return empleadoId(correo)
                .map(id -> areaRepository.findByContactoResponsableId(id).stream()
                        .sorted(Comparator.comparing(AreaEscalamiento::getNombre, String.CASE_INSENSITIVE_ORDER))
                        .map(a -> new AreaRepresentanteDTO.Area(a.getId(), a.getNombre()))
                        .toList())
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<AreaRepresentanteDTO.TicketEscalado> ticketsEscalados(String correo) {
        return empleadoId(correo)
                .map(id -> ticketRepository.findEscaladosPorRepresentante(id).stream().map(this::aDto).toList())
                .orElse(List.of());
    }

    /**
     * El representante responde y el ticket vuelve al técnico (Escalado -> En revisión). Reutiliza el cambio de
     * estado normal, por lo que también reanuda el SLA, deja el historial con el usuario y publica el evento.
     */
    @Transactional
    public void responder(UUID ticketId, String comentario, String correo, String ip) {
        if (comentario == null || comentario.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La respuesta es obligatoria");
        }
        String texto = comentario.trim();
        if (texto.length() > MAX_COMENTARIO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La respuesta no puede superar los " + MAX_COMENTARIO + " caracteres");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));
        Empleado representante = rrhhClientService.obtenerEmpleadoPorCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a este ticket"));

        AreaEscalamiento area = ticket.getAreaEscalada();
        if (area == null || !representante.getId().equals(area.getContactoResponsableId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el representante del área puede responder");
        }
        if (!"Escalado".equals(ticket.getEstado().getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El ticket ya no está escalado");
        }

        Estado enRevision = estadoRepository.findByNombre("En revisión")
                .orElseThrow(() -> new IllegalStateException("Estado 'En revisión' no configurado"));
        String nombreRepresentante = representante.getNombres() + " " + representante.getApellidos();

        CambiarEstadoRequest cambio = new CambiarEstadoRequest();
        cambio.setNuevoEstadoId(enRevision.getId());
        cambio.setComentario("Respuesta del área " + area.getNombre() + " (" + nombreRepresentante + "): " + texto);
        ticketService.actualizarEstado(ticketId, cambio, correo, ip);

        // Aviso al técnico responsable
        Map<String, Object> evento = new HashMap<>();
        evento.put("codigo", ticket.getCodigo());
        evento.put("asunto", ticket.getAsunto());
        evento.put("area", area.getNombre());
        evento.put("representante", nombreRepresentante);
        evento.put("comentario", texto);
        evento.put("correoTecnico", ticket.getTecnicoAsignado() != null
                ? ticket.getTecnicoAsignado().getEmpleado().getCorreoCorporativo() : "");
        kafkaProducerService.enviarEvento("ticket.respuesta_area", evento);

        log.info("↩️ El área {} respondió el ticket {}; vuelve al técnico", area.getNombre(), ticket.getCodigo());
    }

    private Optional<UUID> empleadoId(String correo) {
        return rrhhClientService.obtenerEmpleadoPorCorreo(correo).map(Empleado::getId);
    }

    private AreaRepresentanteDTO.TicketEscalado aDto(Ticket t) {
        var tec = t.getTecnicoAsignado() != null ? t.getTecnicoAsignado().getEmpleado() : null;
        return new AreaRepresentanteDTO.TicketEscalado(
                t.getId(), t.getCodigo(), t.getAsunto(),
                t.getSolicitante().getNombres() + " " + t.getSolicitante().getApellidos(),
                t.getPrioridad().getNombre(), t.getAreaEscalada().getNombre(), t.getMotivoEscalamiento(),
                tec != null ? tec.getNombres() + " " + tec.getApellidos() : null,
                tec != null ? tec.getCorreoCorporativo() : null,
                t.getFechaCreacion(), t.getFechaActualizacion());
    }
}
