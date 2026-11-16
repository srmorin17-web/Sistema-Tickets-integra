package com.fundacioncalma.tickets.backend.seguridad.config;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fundacioncalma.tickets.backend.seguridad.service.AuditoriaAccesoService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Se ejecuta cuando un usuario autenticado intenta usar un endpoint que su rol no permite
 * (@PreAuthorize): registra el intento en usuario.intento_acceso y responde 403.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccesoDenegadoHandler implements AccessDeniedHandler {

    private final AuditoriaAccesoService auditoriaAccesoService;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String correo = (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken))
                ? auth.getName() : null;

        try {
            auditoriaAccesoService.registrarIntentoNoPermitido(
                    correo, request.getRequestURI(), request.getMethod(), request.getRemoteAddr(), "DENEGADO");
        } catch (Exception e) {
            // El registro no debe impedir que se responda el 403
            log.error("❌ No se pudo registrar el intento de acceso a {}: {}", request.getRequestURI(), e.getMessage());
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write("{\"error\":\"Acceso denegado\"}");
    }
}
