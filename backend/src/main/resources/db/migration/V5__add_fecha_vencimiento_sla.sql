ALTER TABLE ticket.ticket ADD COLUMN fecha_vencimiento_sla TIMESTAMPTZ;
CREATE INDEX idx_ticket_fecha_vencimiento ON ticket.ticket(fecha_vencimiento_sla);