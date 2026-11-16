ALTER TABLE usuario.sesion
    ADD COLUMN family_id UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN fecha_ultima_actividad TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN revocada BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX uq_sesion_refresh_token ON usuario.sesion(refresh_token);
CREATE INDEX idx_sesion_family ON usuario.sesion(family_id);
CREATE INDEX idx_sesion_usuario ON usuario.sesion(id_usuario);
