UPDATE integracion.rrhh_sync_estado
SET snapshot_completo = FALSE,
    snapshot_after_id = NULL,
    snapshot_version = NULL
WHERE id = 1;
