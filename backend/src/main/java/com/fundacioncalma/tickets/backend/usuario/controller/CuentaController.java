package com.fundacioncalma.tickets.backend.usuario.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fundacioncalma.tickets.backend.usuario.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/** La cuenta del propio usuario. Es la única ruta permitida mientras tiene una contraseña temporal (ver JwtAuthenticationFilter). */
@RestController
@RequiredArgsConstructor
public class CuentaController {

    public static final String RUTA_CAMBIO_CONTRASENA = "/api/v1/cuenta/contrasena";

    private final AuthService authService;

    public record CambiarContrasenaRequest(String contrasenaActual, String contrasenaNueva) {}

    @PutMapping(RUTA_CAMBIO_CONTRASENA)
    public ResponseEntity<Void> cambiarContrasena(
            @RequestBody CambiarContrasenaRequest request, Authentication authentication, HttpServletRequest http) {
        authService.cambiarContrasena(authentication.getName(), request.contrasenaActual(), request.contrasenaNueva(), http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
