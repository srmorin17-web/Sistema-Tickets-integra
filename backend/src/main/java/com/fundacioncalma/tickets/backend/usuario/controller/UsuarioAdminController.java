package com.fundacioncalma.tickets.backend.usuario.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fundacioncalma.tickets.backend.usuario.dto.UsuarioAdminDTO;
import com.fundacioncalma.tickets.backend.usuario.dto.ProvisionamientoCuentasDTO;
import com.fundacioncalma.tickets.backend.usuario.dto.ProvisionamientoEncoladoDTO;
import com.fundacioncalma.tickets.backend.usuario.service.UsuarioAdminService;
import com.fundacioncalma.tickets.backend.usuario.service.ProvisionamientoCuentasService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.server.ResponseStatusException;

/** Gestión de cuentas. Las reglas finas (qué roles puede asignar cada quien) se aplican en UsuarioAdminService. */
@RestController
@RequiredArgsConstructor
public class UsuarioAdminController {

    private final UsuarioAdminService usuarioAdminService;
    private final ProvisionamientoCuentasService provisionamientoCuentasService;

    public record CrearUsuarioRequest(UUID empleadoId, UUID rolId) {}

    public record ActualizarUsuarioRequest(UUID rolId, Boolean activo) {}
    public record AutomaticoRequest(Boolean automatico) {}

    @GetMapping("/api/v1/admin/usuarios/provisionamiento")
    @PreAuthorize("hasAnyRole('Administrador', 'Superadmin')")
    public ResponseEntity<ProvisionamientoCuentasDTO> obtenerProvisionamiento() {
        return ResponseEntity.ok(provisionamientoCuentasService.estado());
    }

    @PutMapping("/api/v1/admin/usuarios/provisionamiento")
    @PreAuthorize("hasAnyRole('Administrador', 'Superadmin')")
    public ResponseEntity<ProvisionamientoCuentasDTO> actualizarProvisionamiento(
            @RequestBody AutomaticoRequest request,
            Authentication authentication,
            HttpServletRequest http) {
        if (request == null || request.automatico() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar si el aprovisionamiento automático está habilitado");
        }
        return ResponseEntity.ok(provisionamientoCuentasService.actualizarAutomatico(
                request.automatico(), authentication, http.getRemoteAddr()));
    }

    @PostMapping("/api/v1/admin/usuarios/provisionamiento/masivo")
    @PreAuthorize("hasAnyRole('Administrador', 'Superadmin')")
    public ResponseEntity<ProvisionamientoEncoladoDTO> encolarProvisionamientoMasivo(
            Authentication authentication, HttpServletRequest http) {
        long encolados = provisionamientoCuentasService.encolarElegibles(authentication, http.getRemoteAddr());
        return ResponseEntity.accepted().body(new ProvisionamientoEncoladoDTO(encolados));
    }

    @GetMapping("/api/v1/admin/usuarios")
    @PreAuthorize("hasAnyRole('Administrador', 'Superadmin')")
    public ResponseEntity<List<UsuarioAdminDTO.Cuenta>> listar(
            @RequestParam(required = false) String q, Authentication authentication) {
        return ResponseEntity.ok(usuarioAdminService.listar(q, authentication));
    }

    @GetMapping("/api/v1/admin/roles")
    @PreAuthorize("hasAnyRole('Administrador', 'Superadmin')")
    public ResponseEntity<List<UsuarioAdminDTO.Rol>> roles(Authentication authentication) {
        return ResponseEntity.ok(usuarioAdminService.roles(authentication));
    }

    @PostMapping("/api/v1/admin/usuarios")
    @PreAuthorize("hasAnyRole('Administrador', 'Superadmin')")
    public ResponseEntity<UsuarioAdminDTO.Creado> crear(
            @RequestBody CrearUsuarioRequest request, Authentication authentication, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioAdminService.crear(request.empleadoId(), request.rolId(), authentication, http.getRemoteAddr()));
    }

    @PostMapping("/api/v1/admin/usuarios/{id}/reenviar-activacion")
    @PreAuthorize("hasAnyRole('Administrador', 'Superadmin')")
    public ResponseEntity<Void> reenviarActivacion(
            @PathVariable UUID id, Authentication authentication, HttpServletRequest http) {
        usuarioAdminService.reenviarActivacion(id, authentication, http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/v1/admin/usuarios/{id}")
    @PreAuthorize("hasAnyRole('Administrador', 'Superadmin')")
    public ResponseEntity<UsuarioAdminDTO.Cuenta> actualizar(
            @PathVariable UUID id, @RequestBody ActualizarUsuarioRequest request,
            Authentication authentication, HttpServletRequest http) {
        return ResponseEntity.ok(usuarioAdminService.actualizar(
                id, request.rolId(), request.activo(), authentication, http.getRemoteAddr()));
    }

    @PostMapping("/api/v1/admin/usuarios/{id}/restablecer-contrasena")
    @PreAuthorize("hasAnyRole('Administrador', 'Superadmin')")
    public ResponseEntity<UsuarioAdminDTO.ContrasenaTemporal> restablecer(
            @PathVariable UUID id, Authentication authentication, HttpServletRequest http) {
        return ResponseEntity.ok(usuarioAdminService.restablecerContrasena(id, authentication, http.getRemoteAddr()));
    }
}
