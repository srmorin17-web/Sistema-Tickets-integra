-- =====================================================================
-- Bloque 13: gestión de usuarios
-- =====================================================================

-- Cuenta activa/inactiva (desactivar no borra nada: el historial conserva a quién hizo cada cosa)
ALTER TABLE usuario.usuario ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;

-- Obliga a cambiar la contraseña en el siguiente inicio de sesión (contraseñas temporales)
ALTER TABLE usuario.usuario ADD COLUMN debe_cambiar_contrasena BOOLEAN NOT NULL DEFAULT FALSE;

-- La cuenta semilla (V4) usa una contraseña que figura en el repositorio: se obliga a cambiarla en el primer acceso.
-- Además se la eleva a Superadmin si todavía no existe ninguno: es el único rol que puede crear cuentas de
-- Administrador, Director y Superadmin, así que sin esto el sistema no podría gestionarlos nunca.
UPDATE usuario.usuario
   SET debe_cambiar_contrasena = TRUE,
       id_rol = CASE
                    WHEN EXISTS (SELECT 1 FROM usuario.usuario u2 JOIN permiso.rol r2 ON r2.id = u2.id_rol WHERE r2.nombre = 'Superadmin')
                        THEN id_rol
                    ELSE (SELECT id FROM permiso.rol WHERE nombre = 'Superadmin')
                END
 WHERE id_empleado IN (SELECT id FROM empleado.empleado WHERE correo_corporativo = 'admin@fundacioncalma.com');

-- Auditoría de la gestión de cuentas (inalterable, igual que el resto de la auditoría). Nunca guarda contraseñas.
CREATE TABLE usuario.auditoria_usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_actor UUID REFERENCES usuario.usuario(id) ON DELETE RESTRICT,     -- quién ejecutó la acción
    id_afectado UUID REFERENCES usuario.usuario(id) ON DELETE RESTRICT,  -- sobre qué cuenta
    accion VARCHAR(40) NOT NULL,   -- CREAR_USUARIO | CAMBIO_ROL | ACTIVAR | DESACTIVAR | RESTABLECER_CONTRASENA | CAMBIO_CONTRASENA
    detalle VARCHAR(500),
    ip_origen VARCHAR(50),
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_auditoria_usuario_fecha ON usuario.auditoria_usuario(fecha_hora DESC);

CREATE TRIGGER trg_auditoria_usuario_inalterable
    BEFORE UPDATE OR DELETE ON usuario.auditoria_usuario
    FOR EACH ROW EXECUTE FUNCTION public.bloquear_modificacion_auditoria();
CREATE TRIGGER trg_auditoria_usuario_sin_truncate
    BEFORE TRUNCATE ON usuario.auditoria_usuario
    FOR EACH STATEMENT EXECUTE FUNCTION public.bloquear_modificacion_auditoria();
