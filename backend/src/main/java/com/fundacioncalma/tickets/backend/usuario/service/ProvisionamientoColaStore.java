package com.fundacioncalma.tickets.backend.usuario.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fundacioncalma.tickets.backend.usuario.dto.ProvisionamientoCuentasDTO;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProvisionamientoColaStore {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public ProvisionamientoCuentasDTO estado() {
        return jdbcTemplate.queryForObject(
                """
                SELECT c.automatico,
                       (SELECT COUNT(*)
                        FROM empleado.empleado e
                        JOIN integracion.rrhh_empleado_lider hr ON hr.id_empleado = e.id
                        WHERE LOWER(TRIM(e.estado_laboral)) = 'activo'
                          AND NULLIF(TRIM(e.correo_corporativo), '') IS NOT NULL
                          AND NOT EXISTS (SELECT 1 FROM usuario.usuario u WHERE u.id_empleado = e.id)
                       ) AS elegibles,
                       COUNT(*) FILTER (WHERE q.estado = 'PENDIENTE') AS pendientes,
                       COUNT(*) FILTER (WHERE q.estado = 'PROCESANDO') AS procesando,
                       COUNT(*) FILTER (WHERE q.estado = 'COMPLETADO') AS completados,
                       COUNT(*) FILTER (WHERE q.estado = 'FALLIDO') AS fallidos
                FROM usuario.provisionamiento_config c
                LEFT JOIN usuario.provisionamiento_cola q ON TRUE
                WHERE c.id = 1
                GROUP BY c.automatico
                """,
                (rs, rowNum) -> new ProvisionamientoCuentasDTO(
                        rs.getBoolean("automatico"),
                        rs.getLong("elegibles"),
                        rs.getLong("pendientes"),
                        rs.getLong("procesando"),
                        rs.getLong("completados"),
                        rs.getLong("fallidos")));
    }

    @Transactional
    public void actualizarAutomatico(boolean automatico) {
        jdbcTemplate.update(
                """
                UPDATE usuario.provisionamiento_config
                SET automatico = ?, actualizado_en = CURRENT_TIMESTAMP
                WHERE id = 1
                """,
                automatico);
    }

    @Transactional
    public long encolarElegibles() {
        return jdbcTemplate.update(
                """
                INSERT INTO usuario.provisionamiento_cola AS cola
                    (id_empleado, origen, estado, resultado, intentos, ultimo_error, creado_en, actualizado_en)
                SELECT e.id, 'MASIVO', 'PENDIENTE', NULL, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                FROM empleado.empleado e
                JOIN integracion.rrhh_empleado_lider hr ON hr.id_empleado = e.id
                WHERE LOWER(TRIM(e.estado_laboral)) = 'activo'
                  AND NULLIF(TRIM(e.correo_corporativo), '') IS NOT NULL
                  AND NOT EXISTS (SELECT 1 FROM usuario.usuario u WHERE u.id_empleado = e.id)
                ON CONFLICT (id_empleado) DO UPDATE
                   SET origen = 'MASIVO',
                       estado = 'PENDIENTE',
                       resultado = NULL,
                       intentos = 0,
                       ultimo_error = NULL,
                       actualizado_en = CURRENT_TIMESTAMP
                 WHERE cola.estado = 'FALLIDO'
                    OR (cola.estado = 'COMPLETADO' AND cola.resultado = 'OMITIDA')
                """);
    }

    @Transactional
    public void encolarSiAutomatico(UUID empleadoId) {
        jdbcTemplate.update(
                """
                INSERT INTO usuario.provisionamiento_cola AS cola (id_empleado, origen, estado)
                SELECT e.id, 'AUTOMATICO', 'PENDIENTE'
                FROM empleado.empleado e
                JOIN usuario.provisionamiento_config c ON c.id = 1 AND c.automatico = TRUE
                WHERE e.id = ?
                  AND LOWER(TRIM(e.estado_laboral)) = 'activo'
                  AND NULLIF(TRIM(e.correo_corporativo), '') IS NOT NULL
                  AND NOT EXISTS (SELECT 1 FROM usuario.usuario u WHERE u.id_empleado = e.id)
                ON CONFLICT (id_empleado) DO UPDATE
                   SET estado = 'PENDIENTE',
                       resultado = NULL,
                       ultimo_error = NULL,
                       actualizado_en = CURRENT_TIMESTAMP
                 WHERE (cola.estado = 'FALLIDO' AND cola.intentos < 3)
                    OR (cola.estado = 'COMPLETADO' AND cola.resultado = 'OMITIDA')
                """,
                empleadoId);
    }

    @Transactional
    public Optional<ElementoCola> reclamarSiguiente() {
        return jdbcTemplate.query(
                """
                UPDATE usuario.provisionamiento_cola
                   SET estado = 'PROCESANDO',
                       intentos = intentos + 1,
                       actualizado_en = CURRENT_TIMESTAMP
                 WHERE id = (
                     SELECT id
                     FROM usuario.provisionamiento_cola
                     WHERE estado = 'PENDIENTE'
                        OR (estado = 'PROCESANDO'
                            AND actualizado_en < CURRENT_TIMESTAMP - INTERVAL '30 minutes')
                     ORDER BY id
                     FOR UPDATE SKIP LOCKED
                     LIMIT 1
                 )
                RETURNING id, id_empleado
                """,
                rs -> rs.next()
                        ? Optional.of(new ElementoCola(rs.getLong("id"), rs.getObject("id_empleado", UUID.class)))
                        : Optional.empty());
    }

    @Transactional
    public void marcarCompletado(long id, boolean creada) {
        jdbcTemplate.update(
                """
                UPDATE usuario.provisionamiento_cola
                SET estado = 'COMPLETADO', resultado = ?, ultimo_error = NULL,
                    actualizado_en = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                creada ? "CREADA" : "OMITIDA",
                id);
    }

    @Transactional
    public void marcarFallido(long id, String error) {
        jdbcTemplate.update(
                """
                UPDATE usuario.provisionamiento_cola
                SET estado = 'FALLIDO', ultimo_error = ?, actualizado_en = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                error == null ? "Error de aprovisionamiento" : error.substring(0, Math.min(error.length(), 500)),
                id);
    }

    public record ElementoCola(long id, UUID empleadoId) {}
}
