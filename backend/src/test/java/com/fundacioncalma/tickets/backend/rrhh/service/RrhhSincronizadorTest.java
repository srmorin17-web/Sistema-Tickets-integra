package com.fundacioncalma.tickets.backend.rrhh.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fundacioncalma.tickets.backend.rrhh.cliente.EmpleadoRrhh;

class RrhhSincronizadorTest {

    @Test
    void replicaDepartamentoYCamposLaboralesAntesDeGuardarEmpleado() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        RrhhSincronizador sincronizador = new RrhhSincronizador(jdbcTemplate);
        UUID empleadoId = UUID.randomUUID();
        UUID departamentoId = UUID.randomUUID();
        UUID liderId = UUID.randomUUID();
        LocalDate fechaIngreso = LocalDate.of(2020, 3, 15);
        EmpleadoRrhh empleado = new EmpleadoRrhh(
                empleadoId,
                "Ana",
                "Pérez",
                "ana@example.org",
                "ana.personal@example.org",
                "DNI",
                "12345678",
                "Analista",
                "ACTIVO",
                departamentoId,
                "Tecnología",
                fechaIngreso,
                liderId,
                "jefe@example.org");

        sincronizador.guardar(empleado);

        var llamadas = inOrder(jdbcTemplate);
        llamadas.verify(jdbcTemplate).update(
                eq(RrhhSincronizador.SQL_UPSERT_DEPARTAMENTO), eq(departamentoId), eq("Tecnología"));
        llamadas.verify(jdbcTemplate).update(
                eq(RrhhSincronizador.SQL_UPSERT),
                eq(empleadoId),
                eq(departamentoId),
                eq("DNI"),
                eq("12345678"),
                eq("ana.personal@example.org"),
                eq("Ana"),
                eq("Pérez"),
                eq("Analista"),
                eq("ana@example.org"),
                eq("ACTIVO"),
                eq(fechaIngreso));
        llamadas.verify(jdbcTemplate).update(
                eq(RrhhSincronizador.SQL_UPSERT_LIDER), eq(empleadoId), eq(liderId));
        llamadas.verify(jdbcTemplate).update(
                eq(RrhhSincronizador.SQL_APLICAR_LIDERES), eq(empleadoId), eq(empleadoId));
    }

    @Test
    void rechazaEmpleadoSinFechaDeIngreso() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        RrhhSincronizador sincronizador = new RrhhSincronizador(jdbcTemplate);
        EmpleadoRrhh empleado = new EmpleadoRrhh(
                UUID.randomUUID(),
                "Ana",
                "Pérez",
                "ana@example.org",
                "ana.personal@example.org",
                "DNI",
                "12345678",
                "Analista",
                "ACTIVO",
                null,
                null,
                null,
                null,
                null);

        assertThrows(IllegalStateException.class, () -> sincronizador.guardar(empleado));
    }
}
