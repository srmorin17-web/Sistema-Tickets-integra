package com.fundacioncalma.tickets.backend.rrhh.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fundacioncalma.tickets.backend.rrhh.dto.EstadoSincronizacionRrhhDTO;
import com.fundacioncalma.tickets.backend.rrhh.service.RrhhSyncService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/rrhh-sync")
@RequiredArgsConstructor
public class RrhhSyncController {

    private final RrhhSyncService syncService;

    @GetMapping
    @PreAuthorize("hasAnyRole('Administrador', 'Superadmin')")
    public EstadoSincronizacionRrhhDTO obtenerEstado() {
        return syncService.obtenerEstado();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Administrador', 'Superadmin')")
    public EstadoSincronizacionRrhhDTO sincronizarAhora() {
        return syncService.sincronizarAhora();
    }
}
