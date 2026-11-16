-- ==========================================
-- Bloque 15: datos locales para probar escalamiento y reasignación
-- ==========================================
-- Los tres técnicos de prueba usan la contraseña local: Tecnico123!
-- Los representantes de escalamiento son empleados de contacto; no tienen cuenta de acceso al sistema.

INSERT INTO empleado.empleado (
    id, nombres, apellidos, cargo, correo_corporativo, estado_laboral
)
VALUES
    (gen_random_uuid(), 'Laura', 'Pineda', 'Representante de Infraestructura y Redes',
     'representante.infraestructura@fundacioncalma.com', 'ACTIVO'),
    (gen_random_uuid(), 'Mateo', 'Rojas', 'Representante de Aplicaciones y Sistemas',
     'representante.aplicaciones@fundacioncalma.com', 'ACTIVO'),
    (gen_random_uuid(), 'Diego', 'Torres', 'Técnico de Soporte TI',
     'diego.torres@fundacioncalma.com', 'ACTIVO'),
    (gen_random_uuid(), 'Sofía', 'Herrera', 'Técnica de Soporte TI',
     'sofia.herrera@fundacioncalma.com', 'ACTIVO'),
    (gen_random_uuid(), 'Andrés', 'Castillo', 'Técnico de Soporte TI',
     'andres.castillo@fundacioncalma.com', 'ACTIVO')
ON CONFLICT (correo_corporativo) DO NOTHING;

INSERT INTO ticket.area_escalamiento (nombre, descripcion, contacto_responsable_id, activo)
SELECT 'Infraestructura y Redes',
       'Conectividad, redes, equipos, impresoras e infraestructura tecnológica.',
       e.id,
       TRUE
FROM empleado.empleado e
WHERE e.correo_corporativo = 'representante.infraestructura@fundacioncalma.com'
  AND NOT EXISTS (
      SELECT 1 FROM ticket.area_escalamiento a WHERE a.nombre = 'Infraestructura y Redes'
  );

INSERT INTO ticket.area_escalamiento (nombre, descripcion, contacto_responsable_id, activo)
SELECT 'Aplicaciones y Sistemas',
       'Aplicaciones institucionales, sistemas internos, accesos e incidencias de software.',
       e.id,
       TRUE
FROM empleado.empleado e
WHERE e.correo_corporativo = 'representante.aplicaciones@fundacioncalma.com'
  AND NOT EXISTS (
      SELECT 1 FROM ticket.area_escalamiento a WHERE a.nombre = 'Aplicaciones y Sistemas'
  );

INSERT INTO usuario.usuario (
    id, id_rol, id_empleado, contrasena_hash, activo, debe_cambiar_contrasena
)
SELECT gen_random_uuid(),
       r.id,
       e.id,
       '$2b$10$NoUPxFkUsVYMuE32E7bubu06nwc3vx4MTLHhwcmKJxaUEJCKSj6tq',
       TRUE,
       FALSE
FROM empleado.empleado e
CROSS JOIN permiso.rol r
WHERE r.nombre = 'Técnico TI'
  AND e.correo_corporativo IN (
      'diego.torres@fundacioncalma.com',
      'sofia.herrera@fundacioncalma.com',
      'andres.castillo@fundacioncalma.com'
  )
  AND NOT EXISTS (
      SELECT 1 FROM usuario.usuario u WHERE u.id_empleado = e.id
  );
