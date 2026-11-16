package com.fundacioncalma.tickets.backend.usuario.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Vistas de la gestión de usuarios (Administrador, Superadmin). */
public interface UsuarioAdminDTO {

    /**
     * puedeGestionar: el usuario que consulta puede cambiar el rol, activar/desactivar y restablecer la contraseña de esta cuenta.
     * esActual: es la cuenta del propio usuario que consulta.
     */
    record Cuenta(
            UUID id, UUID empleadoId, String nombre, String correo, UUID rolId, String rol,
            boolean activo, boolean activada, boolean debeCambiarContrasena, LocalDateTime ultimoAcceso,
            boolean puedeGestionar, boolean esActual) {}

    /** asignable: el usuario que consulta puede asignar este rol. */
    record Rol(UUID id, String nombre, String descripcion, boolean asignable) {}

    record Creado(Cuenta cuenta) {}

    record ContrasenaTemporal(String contrasenaTemporal) {}
}
