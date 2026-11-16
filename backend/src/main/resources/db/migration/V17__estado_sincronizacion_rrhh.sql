CREATE SCHEMA IF NOT EXISTS integracion;

CREATE TABLE integracion.rrhh_sync_estado (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    cursor_version BIGINT NOT NULL DEFAULT 0,
    snapshot_completo BOOLEAN NOT NULL DEFAULT FALSE,
    snapshot_after_id UUID,
    snapshot_version BIGINT,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    ultima_intento TIMESTAMPTZ,
    ultima_conexion TIMESTAMPTZ,
    ultima_sincronizacion TIMESTAMPTZ,
    ultimo_error VARCHAR(1000),
    registros_aplicados BIGINT NOT NULL DEFAULT 0
);

INSERT INTO integracion.rrhh_sync_estado (id)
VALUES (1);
