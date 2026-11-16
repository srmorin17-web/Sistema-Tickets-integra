-- Agregamos la columna para guardar el SLA histórico al momento de la creación (RF-33)
ALTER TABLE ticket.ticket ADD COLUMN sla_horas_snapshot INT;

COMMENT ON COLUMN ticket.ticket.sla_horas_snapshot IS 'Copia del SLA en horas al momento de crear el ticket. Garantiza que cambios futuros en la configuración no afecten métricas históricas (RF-33).';