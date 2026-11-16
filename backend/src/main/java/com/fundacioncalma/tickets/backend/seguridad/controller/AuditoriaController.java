package com.fundacioncalma.tickets.backend.seguridad.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fundacioncalma.tickets.backend.seguridad.dto.AuditoriaDTO;
import com.fundacioncalma.tickets.backend.seguridad.service.AuditoriaAccesoService;
import com.fundacioncalma.tickets.backend.seguridad.service.AuditoriaUsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaAccesoService auditoriaAccesoService;
    private final AuditoriaUsuarioService auditoriaUsuarioService;

    public record VistaDenegadaRequest(String ruta) {}

    /** El frontend avisa cuando un usuario intenta abrir una pantalla que su rol no permite. */
    @PostMapping("/api/v1/seguridad/intentos-acceso")
    public ResponseEntity<Void> reportarVistaDenegada(
            @RequestBody VistaDenegadaRequest request, Authentication authentication, HttpServletRequest http) {
        if (request == null || request.ruta() == null || request.ruta().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La ruta es obligatoria");
        }
        auditoriaAccesoService.registrarIntentoNoPermitido(
                authentication.getName(), request.ruta(), "GET", http.getRemoteAddr(), "VISTA_DENEGADA");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/auditoria/accesos")
    @PreAuthorize("hasRole('Superadmin')")
    public ResponseEntity<List<AuditoriaDTO.Acceso>> accesos(@RequestParam(required = false) Integer limite) {
        return ResponseEntity.ok(auditoriaAccesoService.listarAccesos(limite));
    }

    @GetMapping("/api/v1/auditoria/intentos")
    @PreAuthorize("hasRole('Superadmin')")
    public ResponseEntity<List<AuditoriaDTO.Intento>> intentos(@RequestParam(required = false) Integer limite) {
        return ResponseEntity.ok(auditoriaAccesoService.listarIntentos(limite));
    }

    @GetMapping("/api/v1/auditoria/usuarios")
    @PreAuthorize("hasRole('Superadmin')")
    public ResponseEntity<List<AuditoriaDTO.GestionUsuario>> gestionUsuarios(@RequestParam(required = false) Integer limite) {
        return ResponseEntity.ok(auditoriaUsuarioService.listar(limite));
    }
}
