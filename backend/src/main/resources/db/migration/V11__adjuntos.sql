-- =====================================================================
-- Bloque 11: adjuntos de tickets (Acta de Refinamiento: Cloudinary, solo se guarda la URL en la BD)
-- =====================================================================

-- Los tipos MIME de Office superan los 50 caracteres (p. ej. ...wordprocessingml.document = 71)
ALTER TABLE ticket.adjunto ALTER COLUMN tipo_mime TYPE VARCHAR(100);

-- Identificador del archivo en el proveedor de almacenamiento (public_id de Cloudinary o ruta local).
-- ruta_cloudinary conserva la URL; public_id es la clave con la que se descarga y se elimina.
ALTER TABLE ticket.adjunto ADD COLUMN public_id VARCHAR(300) NOT NULL DEFAULT '';
ALTER TABLE ticket.adjunto ALTER COLUMN public_id DROP DEFAULT;

-- Política de retención: el archivo se elimina del almacenamiento pero se conserva el registro (nombre, tamaño, quién y cuándo)
ALTER TABLE ticket.adjunto ADD COLUMN purgado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ticket.adjunto ADD COLUMN fecha_purgado TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_adjunto_ticket ON ticket.adjunto(ticket_id);
