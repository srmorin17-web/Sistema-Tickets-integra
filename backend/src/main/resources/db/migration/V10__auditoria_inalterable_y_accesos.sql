-- =====================================================================
-- Bloque 6: auditoría y seguridad reforzada (Acta de Refinamiento, sección 5)
-- =====================================================================

-- 1) Registro de cada inicio de sesión: usuario + IP + fecha y hora + resultado.
--    id_usuario es NULL cuando el correo ingresado no existe.
CREATE TABLE usuario.registro_acceso (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario UUID REFERENCES usuario.usuario(id) ON DELETE RESTRICT,
    correo_intentado VARCHAR(255) NOT NULL,
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_origen VARCHAR(50),
    resultado VARCHAR(30) NOT NULL -- EXITOSO | FALLIDO
);

CREATE INDEX idx_registro_acceso_fecha ON usuario.registro_acceso(fecha_hora DESC);
CREATE INDEX idx_intento_acceso_fecha ON usuario.intento_acceso(fecha_intento DESC);

-- 2) Auditoría inalterable: ni siquiera el Superadmin (ni la aplicación) puede editar o borrar estos registros.
--    Las tablas pasan a ser de solo inserción. Los triggers también bloquean los borrados en cascada,
--    por lo que no se pueden eliminar tickets ni usuarios que ya tengan historial.
CREATE OR REPLACE FUNCTION public.bloquear_modificacion_auditoria() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'La tabla %.% es de solo inserción: no se permite % de registros de auditoría',
        TG_TABLE_SCHEMA, TG_TABLE_NAME, TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_historial_auditoria_inalterable
    BEFORE UPDATE OR DELETE ON ticket.historial_auditoria
    FOR EACH ROW EXECUTE FUNCTION public.bloquear_modificacion_auditoria();
CREATE TRIGGER trg_historial_auditoria_sin_truncate
    BEFORE TRUNCATE ON ticket.historial_auditoria
    FOR EACH STATEMENT EXECUTE FUNCTION public.bloquear_modificacion_auditoria();

CREATE TRIGGER trg_intento_acceso_inalterable
    BEFORE UPDATE OR DELETE ON usuario.intento_acceso
    FOR EACH ROW EXECUTE FUNCTION public.bloquear_modificacion_auditoria();
CREATE TRIGGER trg_intento_acceso_sin_truncate
    BEFORE TRUNCATE ON usuario.intento_acceso
    FOR EACH STATEMENT EXECUTE FUNCTION public.bloquear_modificacion_auditoria();

CREATE TRIGGER trg_registro_acceso_inalterable
    BEFORE UPDATE OR DELETE ON usuario.registro_acceso
    FOR EACH ROW EXECUTE FUNCTION public.bloquear_modificacion_auditoria();
CREATE TRIGGER trg_registro_acceso_sin_truncate
    BEFORE TRUNCATE ON usuario.registro_acceso
    FOR EACH STATEMENT EXECUTE FUNCTION public.bloquear_modificacion_auditoria();
