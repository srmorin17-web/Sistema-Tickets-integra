-- ROLES (Incluyendo Director y Superadmin)
INSERT INTO permiso.rol (id, nombre, descripcion) VALUES 
(gen_random_uuid(), 'Empleado', 'Usuario final que solicita soporte'),
(gen_random_uuid(), 'Técnico TI', 'Encargado de resolver tickets de primer nivel'),
(gen_random_uuid(), 'Supervisor', 'Supervisa métricas, reasigna tickets y genera reportes'),
(gen_random_uuid(), 'Administrador', 'Gestiona configuraciones, usuarios y permisos del sistema'),
(gen_random_uuid(), 'Director', 'Rol directivo con acceso a dashboard ejecutivo y solicitudes de alta criticidad'),
(gen_random_uuid(), 'Superadmin', 'Acceso total al sistema, incluyendo auditoría inalterable');

-- ESTADOS DEL TICKET (Flujo refinado)
INSERT INTO ticket.estado (id, nombre, descripcion, color_hex, orden, es_estado_final, activo) VALUES 
(gen_random_uuid(), 'Recibido', 'El ticket ha sido creado y está en la bandeja', '#E5E7EB', 1, FALSE, TRUE),
(gen_random_uuid(), 'En revisión', 'El técnico está trabajando en la solicitud', '#3B82F6', 2, FALSE, TRUE),
(gen_random_uuid(), 'Requiere información', 'Se espera respuesta del solicitante (SLA en pausa)', '#F59E0B', 3, FALSE, TRUE),
(gen_random_uuid(), 'Resuelto', 'El técnico ha aplicado una solución', '#10B981', 4, TRUE, TRUE),
(gen_random_uuid(), 'Cerrado', 'El ticket se cerró automáticamente (72h) o por confirmación', '#065F46', 5, TRUE, TRUE),
-- Estados legacy desactivados para el nuevo flujo, pero presentes por integridad
(gen_random_uuid(), 'Confirmado', 'Estado legacy (en desuso)', '#93C5FD', 6, FALSE, FALSE),
(gen_random_uuid(), 'Cancelado', 'Estado legacy (en desuso)', '#EF4444', 7, TRUE, FALSE);

-- PRIORIDADES Y SLA (En horas)
INSERT INTO ticket.prioridad (id, nombre, sla_horas) VALUES 
(gen_random_uuid(), 'Crítica', 4),
(gen_random_uuid(), 'Alta', 24),
(gen_random_uuid(), 'Media', 48),
(gen_random_uuid(), 'Baja', 72);

-- CATEGORÍAS INICIALES
INSERT INTO ticket.categoria (id, nombre, descripcion, activo) VALUES 
(gen_random_uuid(), 'Incidencia o error', 'Fallas en sistemas, hardware o software', TRUE),
(gen_random_uuid(), 'Solicitud de cuenta o acceso', 'Altas, bajas o modificaciones de permisos', TRUE),
(gen_random_uuid(), 'Soporte funcional', 'Dudas sobre el uso de herramientas', TRUE),
(gen_random_uuid(), 'Soporte técnico general', 'Otros requerimientos no clasificados', TRUE);