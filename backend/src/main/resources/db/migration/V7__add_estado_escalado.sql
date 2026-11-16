-- Agregar el estado "Escalado" para el flujo de derivación a áreas externas
INSERT INTO ticket.estado (id, nombre, descripcion, color_hex, orden, es_estado_final, activo)
VALUES (gen_random_uuid(), 'Escalado', 'El ticket ha sido derivado a un área externa especializada', '#8B5CF6', 8, FALSE, TRUE);