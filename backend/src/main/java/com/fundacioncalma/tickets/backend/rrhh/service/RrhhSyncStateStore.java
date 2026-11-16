package com.fundacioncalma.tickets.backend.rrhh.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fundacioncalma.tickets.backend.rrhh.dto.EstadoSincronizacionRrhhDTO;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RrhhSyncStateStore {

    private final JdbcTemplate jdbcTemplate;

    public SyncState obtener() {
        return jdbcTemplate.queryForObject(
                """
                SELECT cursor_version, snapshot_completo, snapshot_after_id, snapshot_version
                FROM integracion.rrhh_sync_estado WHERE id = 1
                """,
                (rs, rowNum) -> new SyncState(
                        rs.getLong("cursor_version"),
                        rs.getBoolean("snapshot_completo"),
                        rs.getObject("snapshot_after_id", UUID.class),
                        rs.getObject("snapshot_version", Long.class)));
    }

    public void iniciarIntento() {
        jdbcTemplate.update(
                """
                UPDATE integracion.rrhh_sync_estado
                SET estado = 'CONECTANDO', ultima_intento = CURRENT_TIMESTAMP, ultimo_error = NULL
                WHERE id = 1
                """);
    }

    public boolean reservarSincronizacionManual() {
        return jdbcTemplate.update(
                """
                UPDATE integracion.rrhh_sync_estado
                SET ultima_sincronizacion_manual = CURRENT_TIMESTAMP,
                    estado = 'CONECTANDO', ultima_intento = CURRENT_TIMESTAMP, ultimo_error = NULL
                WHERE id = 1
                  AND estado <> 'CONECTANDO'
                  AND (ultima_sincronizacion_manual IS NULL
                       OR ultima_sincronizacion_manual <= CURRENT_TIMESTAMP - INTERVAL '2 minutes')
                """) == 1;
    }

    public long cooldownManualSegundos() {
        return jdbcTemplate.queryForObject(
                """
                SELECT GREATEST(
                    0,
                    CEIL(EXTRACT(EPOCH FROM (
                        ultima_sincronizacion_manual + INTERVAL '2 minutes' - CURRENT_TIMESTAMP
                    )))
                )::BIGINT
                FROM integracion.rrhh_sync_estado WHERE id = 1
                """,
                Long.class);
    }

    public void registrarConexion() {
        jdbcTemplate.update(
                """
                UPDATE integracion.rrhh_sync_estado
                SET ultima_conexion = CURRENT_TIMESTAMP
                WHERE id = 1
                """);
    }

    public void guardarPaginaSnapshot(UUID afterId, long snapshotVersion, boolean completa, long aplicados) {
        jdbcTemplate.update(
                """
                UPDATE integracion.rrhh_sync_estado
                SET snapshot_after_id = ?, snapshot_version = ?, snapshot_completo = ?,
                    cursor_version = CASE WHEN ? THEN ? ELSE cursor_version END,
                    registros_aplicados = registros_aplicados + ?
                WHERE id = 1
                """,
                completa ? null : afterId,
                completa ? null : snapshotVersion,
                completa,
                completa,
                snapshotVersion,
                aplicados);
    }

    public void guardarCursorCambios(long cursor, long aplicados) {
        jdbcTemplate.update(
                """
                UPDATE integracion.rrhh_sync_estado
                SET cursor_version = ?, registros_aplicados = registros_aplicados + ?
                WHERE id = 1
                """,
                cursor,
                aplicados);
    }

    public void marcarConectada() {
        jdbcTemplate.update(
                """
                UPDATE integracion.rrhh_sync_estado
                SET estado = 'CONECTADA', ultima_sincronizacion = CURRENT_TIMESTAMP, ultimo_error = NULL
                WHERE id = 1
                """);
    }

    public void marcarError(String mensaje) {
        String acotado = mensaje == null ? "Error no especificado" : mensaje.substring(0, Math.min(mensaje.length(), 1000));
        jdbcTemplate.update(
                """
                UPDATE integracion.rrhh_sync_estado
                SET estado = 'ERROR', ultimo_error = ?
                WHERE id = 1
                """,
                acotado);
    }

    public EstadoSincronizacionRrhhDTO estado(boolean habilitada, long intervaloMinutos) {
        return jdbcTemplate.queryForObject(
                """
                SELECT estado, ultima_conexion, ultima_sincronizacion, ultima_intento,
                       ultimo_error, cursor_version, registros_aplicados, ultima_sincronizacion_manual,
                       GREATEST(
                           0,
                           CEIL(EXTRACT(EPOCH FROM (
                               ultima_sincronizacion_manual + INTERVAL '2 minutes' - CURRENT_TIMESTAMP
                           )))
                       )::BIGINT AS cooldown_manual_segundos
                FROM integracion.rrhh_sync_estado WHERE id = 1
                """,
                (rs, rowNum) -> new EstadoSincronizacionRrhhDTO(
                        habilitada,
                        habilitada ? rs.getString("estado") : "DESHABILITADA",
                        instant(rs.getTimestamp("ultima_conexion")),
                        instant(rs.getTimestamp("ultima_sincronizacion")),
                        instant(rs.getTimestamp("ultima_intento")),
                        habilitada ? rs.getString("ultimo_error") : null,
                        rs.getLong("cursor_version"),
                        rs.getLong("registros_aplicados"),
                        intervaloMinutos,
                        instant(rs.getTimestamp("ultima_sincronizacion_manual")),
                        rs.getLong("cooldown_manual_segundos")));
    }

    private Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    public record SyncState(long cursorVersion, boolean snapshotCompleto, UUID snapshotAfterId, Long snapshotVersion) {}
}
