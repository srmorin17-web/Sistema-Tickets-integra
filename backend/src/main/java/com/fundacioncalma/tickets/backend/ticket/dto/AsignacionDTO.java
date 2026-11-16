package com.fundacioncalma.tickets.backend.ticket.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Vistas para la asignación y reasignación de tickets (supervisión). */
public interface AsignacionDTO {

    /** limite: tickets activos máximos por técnico; 0 significa sin límite. */
    record Tecnico(UUID id, String nombre, String correo, long ticketsActivos, int limite) {}

    /** tecnicoId y tecnico son nulos si el ticket aún no tiene técnico asignado. */
    record TicketSupervision(
            UUID id, String codigo, String asunto, String solicitante, String prioridad, String estado,
            UUID tecnicoId, String tecnico, LocalDateTime fechaVencimientoSla, Integer slaHorasSnapshot,
            boolean pausado) {}
}
