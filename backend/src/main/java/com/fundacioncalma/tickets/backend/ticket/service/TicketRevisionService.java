package com.fundacioncalma.tickets.backend.ticket.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fundacioncalma.tickets.backend.ticket.dto.CambiarEstadoRequest;
import com.fundacioncalma.tickets.backend.ticket.event.KafkaProducerService;
import com.fundacioncalma.tickets.backend.ticket.model.Estado;
import com.fundacioncalma.tickets.backend.ticket.model.HistorialAuditoria;
import com.fundacioncalma.tickets.backend.ticket.model.Ticket;
import com.fundacioncalma.tickets.backend.ticket.repository.EstadoRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.HistorialAuditoriaRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.TicketRepository;
import com.fundacioncalma.tickets.backend.usuario.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Revisión opcional del usuario (Acta de Refinamiento, sección 1): tras "Resuelto", el solicitante puede dejar
 * un comentario. Se guarda en el historial de auditoría (REVISION_USUARIO); no cambia el estado del ticket
 * ni reinicia el plazo del cierre automático.
 */
@Service
@RequiredArgsConstructor
public class TicketRevisionService {

    static final String TIPO_REVISION = "REVISION_USUARIO";
    private static final int MAX_COMENTARIO = 1000;

    private final TicketRepository ticketRepository;
    private final HistorialAuditoriaRepository historialRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;
    private final EstadoRepository estadoRepository;
    private final TicketService ticketService;
    private final KafkaProducerService kafkaProducerService;

    @Transactional
    public void registrarRevision(UUID ticketId, String comentario, String correo, String ip) {
        if (comentario == null || comentario.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El comentario es obligatorio");
        }
        String texto = comentario.trim();
        if (texto.length() > MAX_COMENTARIO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El comentario no puede superar los " + MAX_COMENTARIO + " caracteres");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));

        if (!ticket.getSolicitante().getCorreoCorporativo().equalsIgnoreCase(correo)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el solicitante puede comentar su ticket");
        }
        if (!"Resuelto".equals(ticket.getEstado().getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede comentar un ticket resuelto");
        }
        if (historialRepository.existeAccion(ticketId, TIPO_REVISION)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya registró su comentario para este ticket");
        }

        historialRepository.save(HistorialAuditoria.builder()
                .ticket(ticket)
                .usuario(usuarioRepository.findByCorreoCorporativo(correo).orElse(null))
                .tipoAccion(TIPO_REVISION)
                .datosNuevos(json(Map.of("comentario", texto)))
                .timestamp(LocalDateTime.now())
                .ipOrigen(ip)
                .build());
    }

    @Transactional
    public void confirmarSolucion(UUID ticketId, String correo, String ip) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));
        if (!ticket.getSolicitante().getCorreoCorporativo().equalsIgnoreCase(correo)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el solicitante puede cerrar su ticket");
        }
        if (!"Resuelto".equals(ticket.getEstado().getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede confirmar la solución de un ticket resuelto");
        }

        Estado cerrado = estadoRepository.findByNombre("Cerrado")
                .orElseThrow(() -> new IllegalStateException("Estado 'Cerrado' no configurado"));
        String comentario = "Solución confirmada y aceptada por el solicitante.";
        CambiarEstadoRequest cambio = new CambiarEstadoRequest();
        cambio.setNuevoEstadoId(cerrado.getId());
        cambio.setComentario(comentario);
        Ticket ticketCerrado = ticketService.actualizarEstado(ticketId, cambio, correo, ip);
        ticketCerrado.setFechaCierre(LocalDateTime.now());
        ticketCerrado.setComentarioCierre(comentario);
        ticketRepository.save(ticketCerrado);
    }

    @Transactional
    public void solicitarReapertura(UUID ticketId, String motivo, String correo, String ip) {
        if (motivo == null || motivo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indique por qué la solución no resolvió el problema");
        }
        String texto = motivo.trim();
        if (texto.length() > MAX_COMENTARIO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo no puede superar los " + MAX_COMENTARIO + " caracteres");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));
        if (!ticket.getSolicitante().getCorreoCorporativo().equalsIgnoreCase(correo)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el solicitante puede reabrir su ticket");
        }
        if (!"Resuelto".equals(ticket.getEstado().getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede reabrir un ticket resuelto");
        }

        Estado enRevision = estadoRepository.findByNombre("En revisión")
                .orElseThrow(() -> new IllegalStateException("Estado 'En revisión' no configurado"));
        CambiarEstadoRequest cambio = new CambiarEstadoRequest();
        cambio.setNuevoEstadoId(enRevision.getId());
        cambio.setComentario(texto);
        Ticket reabierto = ticketService.actualizarEstado(ticketId, cambio, correo, ip);

        Map<String, Object> evento = Map.of(
                "codigo", reabierto.getCodigo(),
                "asunto", reabierto.getAsunto(),
                "motivo", texto,
                "solicitante", reabierto.getSolicitante().getNombres() + " " + reabierto.getSolicitante().getApellidos(),
                "correoTecnico", reabierto.getTecnicoAsignado() == null
                        ? "" : reabierto.getTecnicoAsignado().getEmpleado().getCorreoCorporativo());
        kafkaProducerService.enviarEvento("ticket.reabierto", evento);
    }

    private String json(Map<String, Object> datos) {
        try {
            return objectMapper.writeValueAsString(datos);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la auditoría", e);
        }
    }
}
