-- Agregar campos para controlar la pausa del SLA
ALTER TABLE ticket.ticket ADD COLUMN pausado BOOLEAN DEFAULT FALSE;
ALTER TABLE ticket.ticket ADD COLUMN fecha_pausa_sla TIMESTAMPTZ;

-- Comentario para documentar
COMMENT ON COLUMN ticket.ticket.pausado IS 'Indica si el SLA está actualmente en pausa (ej. esperando info del usuario)';
COMMENT ON COLUMN ticket.ticket.fecha_pausa_sla IS 'Momento en que se inició la pausa del SLA';