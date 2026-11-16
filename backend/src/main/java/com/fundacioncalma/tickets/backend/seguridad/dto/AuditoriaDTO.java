package com.fundacioncalma.tickets.backend.seguridad.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Vistas de solo lectura de los registros de auditoría de seguridad. */
public interface AuditoriaDTO {

    record Acceso(UUID id, String correo, String resultado, String ip, LocalDateTime fecha) {}

    /** usuario es nulo cuando el intento no estaba autenticado. */
    record Intento(UUID id, String usuario, String ruta, String metodo, String resultado, String ip,
                   LocalDateTime fecha) {}

    /** actor y afectado son correos corporativos; el detalle nunca contiene contraseñas. */
    record GestionUsuario(UUID id, String actor, String afectado, String accion, String detalle, String ip, LocalDateTime fecha) {}
}
