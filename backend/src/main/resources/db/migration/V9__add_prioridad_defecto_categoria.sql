-- La prioridad (y con ella el SLA) de un ticket la define el sistema según su categoría.
ALTER TABLE ticket.categoria
    ADD COLUMN prioridad_defecto_id UUID REFERENCES ticket.prioridad(id);

UPDATE ticket.categoria SET prioridad_defecto_id = (SELECT id FROM ticket.prioridad WHERE nombre = 'Alta')
 WHERE nombre = 'Incidencia o error';
UPDATE ticket.categoria SET prioridad_defecto_id = (SELECT id FROM ticket.prioridad WHERE nombre = 'Media')
 WHERE nombre IN ('Solicitud de cuenta o acceso', 'Soporte funcional');
UPDATE ticket.categoria SET prioridad_defecto_id = (SELECT id FROM ticket.prioridad WHERE nombre = 'Baja')
 WHERE nombre = 'Soporte técnico general';

-- Cualquier otra categoría existente queda con prioridad Media
UPDATE ticket.categoria SET prioridad_defecto_id = (SELECT id FROM ticket.prioridad WHERE nombre = 'Media')
 WHERE prioridad_defecto_id IS NULL;
