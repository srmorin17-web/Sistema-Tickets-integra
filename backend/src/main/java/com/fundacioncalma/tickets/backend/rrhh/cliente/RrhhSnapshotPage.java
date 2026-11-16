package com.fundacioncalma.tickets.backend.rrhh.cliente;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;

public record RrhhSnapshotPage(
        List<EmpleadoRrhh> empleados,
        @JsonAlias("siguiente_id")
        UUID siguienteId,
        @JsonAlias("cursor_cambios")
        long cursorCambios,
        @JsonAlias("hay_mas")
        boolean hayMas) {}
