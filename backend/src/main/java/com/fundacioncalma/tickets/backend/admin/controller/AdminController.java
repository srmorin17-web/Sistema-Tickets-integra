package com.fundacioncalma.tickets.backend.admin.controller;

import com.fundacioncalma.tickets.backend.admin.dto.*;
import com.fundacioncalma.tickets.backend.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // Solo el rol Administrador o Superadmin puede acceder a estos endpoints
    @GetMapping("/catalogos")
    @PreAuthorize("hasAnyRole('Administrador', 'Superadmin')")
    public ResponseEntity<AdminCatalogosResponse> obtenerCatalogos() {
        return ResponseEntity.ok(adminService.obtenerCatalogos());
    }

    @PostMapping("/categorias")
    @PreAuthorize("hasAnyRole('Administrador', 'Superadmin')")
    public ResponseEntity<?> crearCategoria(@RequestBody CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.crearCategoria(request));
    }

    @PutMapping("/categorias/{id}")
    @PreAuthorize("hasAnyRole('Administrador', 'Superadmin')")
    public ResponseEntity<?> actualizarCategoria(@PathVariable UUID id, @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(adminService.actualizarCategoria(id, request));
    }

    @PutMapping("/prioridades/{id}")
    @PreAuthorize("hasAnyRole('Administrador', 'Superadmin')")
    public ResponseEntity<?> actualizarPrioridad(@PathVariable UUID id, @RequestBody PrioridadRequest request) {
        return ResponseEntity.ok(adminService.actualizarPrioridad(id, request));
    }

    @PostMapping("/areas-escalamiento")
    @PreAuthorize("hasAnyRole('Administrador', 'Superadmin')")
    public ResponseEntity<?> crearArea(@RequestBody AreaEscalamientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.crearArea(request));
    }

    @PutMapping("/areas-escalamiento/{id}")
    @PreAuthorize("hasAnyRole('Administrador', 'Superadmin')")
    public ResponseEntity<?> actualizarArea(@PathVariable UUID id, @RequestBody AreaEscalamientoRequest request) {
        return ResponseEntity.ok(adminService.actualizarArea(id, request));
    }
}
