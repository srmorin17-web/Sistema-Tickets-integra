\set ON_ERROR_STOP off
\echo '--- 1) alta de un empleado nuevo'
INSERT INTO empleado.empleado (id, nombres, apellidos, cargo, correo_corporativo, estado_laboral, fecha_modificacion)
            VALUES ('00000000-0000-0000-0000-0000000000d1', 'María', 'Pérez', 'Analista de Sistemas', 'maria.perez@calma.org', 'ACTIVO', CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE SET
                nombres = EXCLUDED.nombres,
                apellidos = EXCLUDED.apellidos,
                cargo = EXCLUDED.cargo,
                correo_corporativo = EXCLUDED.correo_corporativo,
                estado_laboral = EXCLUDED.estado_laboral,
                fecha_modificacion = CURRENT_TIMESTAMP;
\echo '--- 2) actualización: cambia de cargo y estado'
INSERT INTO empleado.empleado (id, nombres, apellidos, cargo, correo_corporativo, estado_laboral, fecha_modificacion)
            VALUES ('00000000-0000-0000-0000-0000000000d1', 'María', 'Pérez Soto', 'Coordinadora', 'maria.perez@calma.org', 'INACTIVO', CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE SET
                nombres = EXCLUDED.nombres,
                apellidos = EXCLUDED.apellidos,
                cargo = EXCLUDED.cargo,
                correo_corporativo = EXCLUDED.correo_corporativo,
                estado_laboral = EXCLUDED.estado_laboral,
                fecha_modificacion = CURRENT_TIMESTAMP;
\echo '--- 3) empleado local existente (con DNI y líder): se actualiza sin perder DNI ni líder'
INSERT INTO empleado.empleado (id, nombres, apellidos, cargo, correo_corporativo, estado_laboral, fecha_modificacion)
            VALUES ('00000000-0000-0000-0000-0000000000a2', 'Ana', 'Torres Rivas', 'Analista Senior', 'ana@calma.org', 'ACTIVO', CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE SET
                nombres = EXCLUDED.nombres,
                apellidos = EXCLUDED.apellidos,
                cargo = EXCLUDED.cargo,
                correo_corporativo = EXCLUDED.correo_corporativo,
                estado_laboral = EXCLUDED.estado_laboral,
                fecha_modificacion = CURRENT_TIMESTAMP;
\echo '--- 4) CONFLICTO: otro id con el correo de ana@calma.org (debe fallar)'
INSERT INTO empleado.empleado (id, nombres, apellidos, cargo, correo_corporativo, estado_laboral, fecha_modificacion)
            VALUES ('00000000-0000-0000-0000-0000000000d9', 'Ana', 'Otra', 'X', 'ana@calma.org', 'ACTIVO', CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE SET
                nombres = EXCLUDED.nombres,
                apellidos = EXCLUDED.apellidos,
                cargo = EXCLUDED.cargo,
                correo_corporativo = EXCLUDED.correo_corporativo,
                estado_laboral = EXCLUDED.estado_laboral,
                fecha_modificacion = CURRENT_TIMESTAMP;
\echo '--- 5) segundo empleado sin DNI (los NULL no chocan en UNIQUE)'
INSERT INTO empleado.empleado (id, nombres, apellidos, cargo, correo_corporativo, estado_laboral, fecha_modificacion)
            VALUES ('00000000-0000-0000-0000-0000000000d2', 'Mateo', 'Ramos', 'Analista', 'mateo@calma.org', 'ACTIVO', CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE SET
                nombres = EXCLUDED.nombres,
                apellidos = EXCLUDED.apellidos,
                cargo = EXCLUDED.cargo,
                correo_corporativo = EXCLUDED.correo_corporativo,
                estado_laboral = EXCLUDED.estado_laboral,
                fecha_modificacion = CURRENT_TIMESTAMP;
\echo '--- resultado'
SELECT correo_corporativo, cargo, estado_laboral, dni IS NULL AS sin_dni, lider_id IS NOT NULL AS con_lider, fecha_modificacion IS NOT NULL AS con_fecha_mod FROM empleado.empleado WHERE correo_corporativo IN ('maria.perez@calma.org','ana@calma.org','mateo@calma.org') ORDER BY 1;
\echo '--- los empleados sincronizados sirven para las claves foráneas (representante de un área y solicitante de un ticket)'
INSERT INTO ticket.area_escalamiento (nombre, contacto_responsable_id) VALUES ('Infraestructura', '00000000-0000-0000-0000-0000000000d1') RETURNING nombre;
INSERT INTO ticket.ticket (codigo, asunto, descripcion, solicitante_id, categoria_id, prioridad_id, estado_id)
SELECT 'TKT-SYNC', 'Prueba', 'x', '00000000-0000-0000-0000-0000000000d2', (SELECT id FROM ticket.categoria LIMIT 1), (SELECT id FROM ticket.prioridad LIMIT 1), (SELECT id FROM ticket.estado WHERE nombre='Recibido') RETURNING codigo;
