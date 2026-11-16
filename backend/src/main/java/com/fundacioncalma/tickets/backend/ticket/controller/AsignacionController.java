package com.fundacioncalma.tickets.backend.ticket.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fundacioncalma.tickets.backend.ticket.dto.AsignacionDTO;
import com.fundacioncalma.tickets.backend.ticket.service.AsignacionService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/** Supervisión de la carga y reasignación manual de tickets (Supervisor, Administrador y Superadmin). */
@RestController
@RequiredArgsConstructor
public class AsignacionController {

    private final AsignacionService asignacionService;

    public record ReasignacionRequest(UUID tecnicoId, String motivo) {}

    @GetMapping("/api/v1/tecnicos")
    @PreAuthorize("hasAnyRole('Supervisor', 'Administrador', 'Superadmin')")
    public ResponseEntity<List<AsignacionDTO.Tecnico>> tecnicos() {
        return ResponseEntity.ok(asignacionService.listarTecnicos());
    }

    /** Todos los tickets activos (incluye los sin asignar), del menor tiempo restante de SLA al mayor. */
    @GetMapping("/api/v1/tickets/supervision")
    @PreAuthorize("hasAnyRole('Supervisor', 'Administrador', 'Superadmin')")
    public ResponseEntity<List<AsignacionDTO.TicketSupervision>> supervision() {
        return ResponseEntity.ok(asignacionService.listarTicketsActivos());
    }

    @PutMapping("/api/v1/tickets/{id}/asignacion")
    @PreAuthorize("hasAnyRole('Supervisor', 'Administrador', 'Superadmin')")
    public ResponseEntity<AsignacionDTO.TicketSupervision> reasignar(
            @PathVariable UUID id, @RequestBody ReasignacionRequest request,
            Authentication authentication, HttpServletRequest http) {
        return ResponseEntity.ok(asignacionService.reasignar(
                id, request.tecnicoId(), request.motivo(), authentication.getName(), http.getRemoteAddr()));
    }
}
