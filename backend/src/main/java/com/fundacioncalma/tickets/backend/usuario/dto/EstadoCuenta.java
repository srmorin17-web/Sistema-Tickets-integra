package com.fundacioncalma.tickets.backend.usuario.dto;

/** Estado actual de una cuenta, leído de la base de datos en cada petición (el token no se usa para decidir el rol ni el estado). */
public record EstadoCuenta(boolean activo, boolean activada, boolean debeCambiarContrasena, String rol) {}
