package com.fundacioncalma.tickets.backend.usuario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String rol;
    private String nombreCompleto;
    // Contraseña temporal: el frontend debe llevar al usuario a cambiarla antes de continuar
    private boolean debeCambiarContrasena;
}
