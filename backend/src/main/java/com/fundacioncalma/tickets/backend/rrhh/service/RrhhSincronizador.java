package com.fundacioncalma.tickets.backend.rrhh.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.fundacioncalma.tickets.backend.rrhh.cliente.EmpleadoRrhh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mantiene la réplica local de empleados (empleado.empleado). Los tickets, usuarios y áreas de escalamiento
 * tienen claves foráneas hacia esa tabla, así que un empleado de RR. HH. debe existir localmente antes de usarse.
 * El id de RR. HH. es el identificador canónico: se inserta tal cual y se actualiza si ya existe.
 * La jerarquía se conserva por separado para resolver referencias aunque el jefe aún no tenga réplica local.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RrhhSincronizador {

    static final String SQL_UPSERT = """
            INSERT INTO empleado.empleado (
                id, id_departamento, tipo_documento, dni, correo_personal, nombres, apellidos, cargo,
                correo_corporativo, estado_laboral, fecha_ingreso, fecha_modificacion
            )
            VALUES (?, CAST(? AS UUID), ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS DATE), CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE SET
                id_departamento = EXCLUDED.id_departamento,
                tipo_documento = EXCLUDED.tipo_documento,
                dni = EXCLUDED.dni,
                correo_personal = EXCLUDED.correo_personal,
                nombres = EXCLUDED.nombres,
                apellidos = EXCLUDED.apellidos,
                cargo = EXCLUDED.cargo,
                correo_corporativo = EXCLUDED.correo_corporativo,
                estado_laboral = EXCLUDED.estado_laboral,
                fecha_ingreso = EXCLUDED.fecha_ingreso,
                fecha_modificacion = CURRENT_TIMESTAMP
            """;

    static final String SQL_UPSERT_LIDER = """
            INSERT INTO integracion.rrhh_empleado_lider (id_empleado, id_lider)
            VALUES (?, ?)
            ON CONFLICT (id_empleado) DO UPDATE SET id_lider = EXCLUDED.id_lider
            """;

    static final String SQL_APLICAR_LIDERES = """
            UPDATE empleado.empleado e
            SET lider_id = CASE
                WHEN m.id_lider IS NOT NULL
                 AND EXISTS (SELECT 1 FROM empleado.empleado jefe WHERE jefe.id = m.id_lider)
                THEN m.id_lider
                ELSE NULL
            END
            FROM integracion.rrhh_empleado_lider m
            WHERE m.id_empleado = e.id
              AND (m.id_empleado = ? OR m.id_lider = ?)
              AND e.lider_id IS DISTINCT FROM CASE
                  WHEN m.id_lider IS NOT NULL
                   AND EXISTS (SELECT 1 FROM empleado.empleado jefe WHERE jefe.id = m.id_lider)
                  THEN m.id_lider
                  ELSE NULL
              END
            """;

    static final String SQL_UPSERT_DEPARTAMENTO = """
            INSERT INTO organigrama.departamento (id, nombre)
            VALUES (?, ?)
            ON CONFLICT (id) DO UPDATE SET nombre = EXCLUDED.nombre
            """;

    private final JdbcTemplate jdbcTemplate;

    public void guardar(EmpleadoRrhh e) {
        if (e.id() == null || vacio(e.nombres()) || vacio(e.apellidos())
                || vacio(e.correoCorporativo()) || e.fechaIngreso() == null) {
            throw new IllegalStateException("RR. HH. entregó un empleado sin id, nombre, correo o fecha de ingreso");
        }
        try {
            if (e.idDepartamento() != null) {
                if (vacio(e.nombreDepartamento())) {
                    throw new IllegalStateException("RR. HH. entregó un departamento sin nombre");
                }
                jdbcTemplate.update(SQL_UPSERT_DEPARTAMENTO, e.idDepartamento(), e.nombreDepartamento().trim());
            }
            jdbcTemplate.update(SQL_UPSERT, e.id(), e.idDepartamento(), e.tipoDocumento(),
                    e.numeroDocumento(), e.correoPersonal(), e.nombres().trim(), e.apellidos().trim(),
                    vacio(e.cargo()) ? "No especificado" : e.cargo().trim(),
                    e.correoCorporativo().trim(), vacio(e.estadoLaboral()) ? "ACTIVO" : e.estadoLaboral().trim(),
                    e.fechaIngreso());
            jdbcTemplate.update(SQL_UPSERT_LIDER, e.id(), e.liderId());
            jdbcTemplate.update(SQL_APLICAR_LIDERES, e.id(), e.id());
        } catch (DataIntegrityViolationException ex) {
            // A report may collide with a local unique constraint such as email or document number.
            log.error("❌ No se pudo sincronizar al empleado {} ({}): {}", e.id(), e.correoCorporativo(), ex.getMostSpecificCause().getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Los datos del empleado entran en conflicto con un registro local; reconcilie correo, documento e identificador con RR. HH.");
        }
    }

    public void desactivar(java.util.UUID id) {
        jdbcTemplate.update(
                """
                UPDATE empleado.empleado
                SET estado_laboral = 'INACTIVO', fecha_modificacion = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                id);
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
