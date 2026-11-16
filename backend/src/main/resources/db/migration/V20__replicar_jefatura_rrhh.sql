CREATE TABLE integracion.rrhh_empleado_lider (
    id_empleado UUID PRIMARY KEY REFERENCES empleado.empleado(id) ON DELETE CASCADE,
    id_lider UUID
);

INSERT INTO integracion.rrhh_empleado_lider (id_empleado, id_lider)
SELECT id, lider_id
FROM empleado.empleado
ON CONFLICT (id_empleado) DO NOTHING;

UPDATE integracion.rrhh_sync_estado
SET snapshot_completo = FALSE,
    snapshot_after_id = NULL,
    snapshot_version = NULL
WHERE id = 1;
