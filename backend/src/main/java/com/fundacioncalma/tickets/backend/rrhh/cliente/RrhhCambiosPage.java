package com.fundacioncalma.tickets.backend.rrhh.cliente;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

public record RrhhCambiosPage(
        List<RrhhCambioEmpleado> cambios,
        @JsonAlias("cursor_cambios")
        long cursor,
        @JsonAlias("hay_mas")
        boolean hayMas) {}
