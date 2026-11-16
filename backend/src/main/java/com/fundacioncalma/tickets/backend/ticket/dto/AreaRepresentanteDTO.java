package com.fundacioncalma.tickets.backend.ticket.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Vistas para el representante de un área de escalamiento. */
public interface AreaRepresentanteDTO {

    record Area(UUID id, String nombre) {}

    /** Ticket escalado al área del representante. tecnico/correoTecnico son nulos si el ticket no tiene técnico. */
    record TicketEscalado(
            UUID id, String codigo, String asunto, String solicitante, String prioridad, String area,
            String motivo, String tecnico, String correoTecnico, LocalDateTime fechaCreacion,
            LocalDateTime fechaEscalamiento) {}
}
