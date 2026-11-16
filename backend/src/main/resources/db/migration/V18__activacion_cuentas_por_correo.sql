ALTER TABLE usuario.usuario
    ADD COLUMN activada BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE usuario.activacion_token (
    token_hash CHAR(64) PRIMARY KEY,
    id_usuario UUID NOT NULL REFERENCES usuario.usuario(id) ON DELETE CASCADE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vence_en TIMESTAMPTZ NOT NULL,
    consumido_en TIMESTAMPTZ
);

CREATE INDEX idx_activacion_token_usuario
    ON usuario.activacion_token(id_usuario)
    WHERE consumido_en IS NULL;
