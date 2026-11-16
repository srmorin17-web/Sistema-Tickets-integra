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
                           jsonb_build_object('estado', 'Cerrado', 'comentario', 'Cierre automático tras 72 horas sin respuesta del usuario.'::text)
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
            