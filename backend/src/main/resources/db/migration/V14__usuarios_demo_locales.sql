-- ==========================================
-- Bloque 14: usuarios de demo para desarrollo local
-- ==========================================
-- Credenciales de prueba (válidas para inicio de sesión local):
--   Admin     -> admin@fundacioncalma.com / Admin123!
--   Técnico   -> tecnico@fundacioncalma.com / Tecnico123!
--   Empleado  -> empleado@fundacioncalma.com / Usuario123!

INSERT INTO empleado.empleado (id, id_departamento, dni, nombres, apellidos, cargo, correo_corporativo, estado_laboral, fecha_ingreso)
VALUES
    (gen_random_uuid(), NULL, '10000001', 'Admin', 'Sistema', 'Administrador General', 'admin@fundacioncalma.com', 'ACTIVO', CURRENT_DATE),
    (gen_random_uuid(), NULL, '20000001', 'Ana', 'García', 'Especialista de Soporte TI', 'tecnico@fundacioncalma.com', 'ACTIVO', CURRENT_DATE),
    (gen_random_uuid(), NULL, '30000001', 'Carlos', 'Mendoza', 'Usuario Final', 'empleado@fundacioncalma.com', 'ACTIVO', CURRENT_DATE)
ON CONFLICT (correo_corporativo) DO NOTHING;

INSERT INTO usuario.usuario (id, id_rol, id_empleado, contrasena_hash, activo, debe_cambiar_contrasena)
VALUES
    (gen_random_uuid(),
     (SELECT id FROM permiso.rol WHERE nombre = 'Superadmin'),
     (SELECT id FROM empleado.empleado WHERE correo_corporativo = 'admin@fundacioncalma.com'),
     '$2b$10$x8kNR62CFqd5Th1hrYIIpe67Einn2n.0KaC.oYX6rGay3RoBZXPPm',
     TRUE,
     FALSE),
    (gen_random_uuid(),
     (SELECT id FROM permiso.rol WHERE nombre = 'Técnico TI'),
     (SELECT id FROM empleado.empleado WHERE correo_corporativo = 'tecnico@fundacioncalma.com'),
     '$2b$10$NoUPxFkUsVYMuE32E7bubu06nwc3vx4MTLHhwcmKJxaUEJCKSj6tq',
     TRUE,
     FALSE),
    (gen_random_uuid(),
     (SELECT id FROM permiso.rol WHERE nombre = 'Empleado'),
     (SELECT id FROM empleado.empleado WHERE correo_corporativo = 'empleado@fundacioncalma.com'),
     '$2b$10$rjvRU8CHkLR2.YoQt0j3aeDZ1v.9H1RhKD/pZufFOf3sXYtnaD4J6',
     TRUE,
     FALSE)
ON CONFLICT (id_empleado) DO NOTHING;
