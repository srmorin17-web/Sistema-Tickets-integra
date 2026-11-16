package com.fundacioncalma.tickets.backend.usuario.dto;

import lombok.Data;
@Data
public class AuthRequest {
    private String correo;
    private String contrasena;
}
