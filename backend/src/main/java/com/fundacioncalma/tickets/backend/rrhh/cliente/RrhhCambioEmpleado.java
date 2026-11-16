package com.fundacioncalma.tickets.backend.rrhh.cliente;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;

public record RrhhCambioEmpleado(
        long version,
        String tipo,
        @JsonAlias("id_empleado")
        UUID idEmpleado,
        EmpleadoRrhh empleado) {}
