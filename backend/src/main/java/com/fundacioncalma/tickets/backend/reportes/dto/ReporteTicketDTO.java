package com.fundacioncalma.tickets.backend.reportes.dto;

public record ReporteTicketDTO(
        String codigo,
        String asunto,
        String solicitante,
        String tecnico,
        String categoria,
        String prioridad,
        String estado,
        String fechaCreacion,
        String fechaCierre,
        String tiempoResolucion,
        String slaCumplido) {}
