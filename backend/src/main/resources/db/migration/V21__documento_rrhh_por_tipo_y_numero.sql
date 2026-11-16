ALTER TABLE empleado.empleado
    ADD COLUMN tipo_documento VARCHAR(20);

ALTER TABLE empleado.empleado
    DROP CONSTRAINT IF EXISTS empleado_dni_key;

ALTER TABLE empleado.empleado
    ADD CONSTRAINT uk_empleado_tipo_documento_dni UNIQUE (tipo_documento, dni);

UPDATE empleado.empleado
SET dni = NULL
WHERE correo_corporativo = 'admin@fundacioncalma.com'
  AND dni = '12345678';

UPDATE integracion.rrhh_sync_estado
SET snapshot_completo = FALSE,
    snapshot_after_id = NULL,
    snapshot_version = NULL
WHERE id = 1;
