package com.fundacioncalma.tickets.backend.ticket.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fundacioncalma.tickets.backend.ticket.event.KafkaProducerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketAutoCloseScheduler {

    private static final String COMENTARIO_CIERRE = "Cierre automático tras 72 horas sin respuesta del usuario.";

    private final JdbcTemplate jdbcTemplate;
    private final KafkaProducerService kafkaProducerService;

    /**
     * Se ejecuta cada hora en punto (minuto 0, segundo 0).
     * Busca tickets en estado 'Resuelto' cuya fecha de actualización sea >= 72 horas atrás
     * y los cambia automáticamente a 'Cerrado', dejando el evento en el historial de auditoría
     * (sin usuario: lo ejecuta el sistema) y avisando al solicitante.
     */
    @Scheduled(cron = "0 0 * * * ?") // Cada hora en punto
    @Transactional
    public void cerrarTicketsResueltosAutomaticamente() {
        log.info("🕒 Iniciando tarea programada: Cierre automático de tickets resueltos...");

        // Una sola sentencia atómica: cierra los tickets, registra cada cierre en el historial y devuelve
        // los datos necesarios para avisar al solicitante. Las sentencias de datos del WITH se ejecutan
        // siempre por completo, aunque la consulta final no lea su resultado.
        String sql = """
            WITH resuelto AS (SELECT id FROM ticket.estado WHERE nombre = 'Resuelto'),
                 cerrado  AS (SELECT id FROM ticket.estado WHERE nombre = 'Cerrado'),
                 cerrados AS (
                    UPDATE ticket.ticket
                       SET estado_id = (SELECT id FROM cerrado),
                           fecha_actualizacion = CURRENT_TIMESTAMP,
                           fecha_cierre = CURRENT_TIMESTAMP,
                           comentario_cierre = COALESCE(comentario_cierre, 'Cerrado automáticamente por inactividad del solicitante tras 72 horas.')
                     WHERE estado_id = (SELECT id FROM resuelto)
                       AND fecha_actualizacion <= CURRENT_TIMESTAMP - INTERVAL '72 hours'
                 RETURNING id, codigo, asunto, solicitante_id, tecnico_asignado_id),
                 auditados AS (
                    INSERT INTO ticket.historial_auditoria (ticket_id, tipo_accion, estado_anterior_id, estado_nuevo_id, datos_nuevos)
                    SELECT c.id, 'CAMBIO_ESTADO', (SELECT id FROM resuelto), (SELECT id FROM cerrado),
                           jsonb_build_object('estado', 'Cerrado', 'comentario', ?::text)
                      FROM cerrados c
                 RETURNING ticket_id)
            SELECT c.id::text AS id, c.codigo, c.asunto,
                   s.correo_corporativo AS correo_solicitante,
                   COALESCE(t.correo_corporativo, '') AS correo_tecnico,
                   COALESCE(t.nombres || ' ' || t.apellidos, '') AS nombre_tecnico
              FROM cerrados c
              JOIN empleado.empleado s ON s.id = c.solicitante_id
              LEFT JOIN usuario.usuario u ON u.id = c.tecnico_asignado_id
              LEFT JOIN empleado.empleado t ON t.id = u.id_empleado
            """;

        try {
            List<Map<String, Object>> cerrados = jdbcTemplate.queryForList(sql, COMENTARIO_CIERRE);
            if (cerrados.isEmpty()) {
                log.info("ℹ️ No se encontraron tickets elegibles para cierre automático.");
                return;
            }
            log.info("✅ Se cerraron automáticamente {} tickets por inactividad.", cerrados.size());

            for (Map<String, Object> fila : cerrados) {
                Map<String, Object> evento = new HashMap<>();
                evento.put("ticketId", fila.get("id"));
                evento.put("codigo", fila.get("codigo"));
                evento.put("asunto", fila.get("asunto"));
                evento.put("nuevoEstado", "Cerrado");
                evento.put("estadoAnterior", "Resuelto");
                evento.put("comentario", COMENTARIO_CIERRE);
                evento.put("correoSolicitante", fila.get("correo_solicitante"));
                evento.put("correoTecnico", fila.get("correo_tecnico"));
                evento.put("nombreTecnico", fila.get("nombre_tecnico"));
                kafkaProducerService.enviarEvento("ticket.estado_cambiado", evento);
            }
        } catch (Exception e) {
            log.error("❌ Error al ejecutar el cierre automático de tickets: {}", e.getMessage());
            throw e; // revierte la transacción: no puede quedar un cierre sin su registro de auditoría
        }
    }
}
