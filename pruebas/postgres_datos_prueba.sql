INSERT INTO empleado.empleado (id, lider_id, dni, nombres, apellidos, cargo, correo_corporativo, fecha_ingreso, estado_laboral) VALUES
 ('00000000-0000-0000-0000-0000000000a1', NULL, '10000001', 'Jorge', 'Lider Paz', 'Jefe de Operaciones', 'jorge@calma.org', '2020-01-01', 'Activo'),
 ('00000000-0000-0000-0000-0000000000a2', '00000000-0000-0000-0000-0000000000a1', '10000002', 'Ana', 'Torres Rivas', 'Analista', 'ana@calma.org', '2021-01-01', 'Activo'),
 ('00000000-0000-0000-0000-0000000000a3', NULL, '10000003', 'Diego', 'Rojas Quispe', 'Técnico de soporte', 'diego@calma.org', '2022-01-01', 'Activo');
INSERT INTO usuario.usuario (id, id_rol, id_empleado, contrasena_hash) VALUES
 ('00000000-0000-0000-0000-0000000000b3', (SELECT id FROM permiso.rol WHERE nombre='Técnico TI'), '00000000-0000-0000-0000-0000000000a3', 'x');
INSERT INTO ticket.ticket (id, codigo, asunto, descripcion, solicitante_id, tecnico_asignado_id, categoria_id, prioridad_id, estado_id, fecha_actualizacion, pausado)
SELECT t.id, t.codigo, t.asunto, 'desc', '00000000-0000-0000-0000-0000000000a2', '00000000-0000-0000-0000-0000000000b3',
       (SELECT id FROM ticket.categoria LIMIT 1), (SELECT id FROM ticket.prioridad WHERE nombre='Alta'),
       (SELECT id FROM ticket.estado WHERE nombre = t.estado), CURRENT_TIMESTAMP - t.antiguedad, false
FROM (VALUES ('00000000-0000-0000-0000-0000000000c1'::uuid, 'TKT-OLD', 'Resuelto hace 80 h', 'Resuelto', INTERVAL '80 hours'),
             ('00000000-0000-0000-0000-0000000000c2'::uuid, 'TKT-NEW', 'Resuelto hace 10 h', 'Resuelto', INTERVAL '10 hours'),
             ('00000000-0000-0000-0000-0000000000c3'::uuid, 'TKT-REV', 'En revisión hace 200 h', 'En revisión', INTERVAL '200 hours')) AS t(id, codigo, asunto, estado, antiguedad);
