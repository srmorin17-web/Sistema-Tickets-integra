package com.fundacioncalma.tickets.backend.ticket.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Vista completa de un ticket. En los listados ("mis tickets") el campo
 * {@code historial} es null; en el detalle viene ordenado cronológicamente.
 */
public record TicketDetalleDTO(
        UUID id,
        String codigo,
        String asunto,
        String descripcion,
        String solicitante,
        String correoSolicitante,
        String tecnicoAsignado,
        String categoria,
        String prioridad,
        UUID estadoId,
        String estado,
        String areaEscalada,
        String representanteArea,
        String motivoEscalamiento,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion,
        LocalDateTime fechaVencimientoSla,
        Integer slaHorasSnapshot,
        boolean pausado,
        LocalDateTime fechaCierre,
        String comentarioCierre,
        List<HistorialItem> historial) {

    /** Evento de auditoría ya resuelto a nombres legibles. usuario: quién ejecutó la acción (nulo para el solicitante). */
    public record HistorialItem(
            String tipoAccion,
            String estadoAnterior,
            String estadoNuevo,
            String comentario,
            String area,
            String tecnico,
            String usuario,
            LocalDateTime fecha) {}
}
