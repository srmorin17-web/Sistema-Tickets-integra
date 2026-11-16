-- Insertar un empleado de prueba (simulando lo que vendría de RRHH)
INSERT INTO empleado.empleado (id, id_departamento, dni, nombres, apellidos, cargo, correo_corporativo, estado_laboral, fecha_ingreso)
VALUES 
(gen_random_uuid(), (SELECT id FROM organigrama.departamento LIMIT 1), '12345678', 'Admin', 'Sistema', 'Administrador General', 'admin@fundacioncalma.com', 'Activo', CURRENT_DATE);

-- Insertar el usuario vinculado a ese empleado con rol de Administrador y contraseña 'Admin123!'
INSERT INTO usuario.usuario (id, id_rol, id_empleado, contrasena_hash)
VALUES 
(gen_random_uuid(), 
 (SELECT id FROM permiso.rol WHERE nombre = 'Administrador'), 
 (SELECT id FROM empleado.empleado WHERE correo_corporativo = 'admin@fundacioncalma.com'), 
 '$2b$10$x8kNR62CFqd5Th1hrYIIpe67Einn2n.0KaC.oYX6rGay3RoBZXPPm');