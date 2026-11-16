package com.fundacioncalma.tickets.backend.ticket.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fundacioncalma.tickets.backend.ticket.dto.AreaRepresentanteDTO;
import com.fundacioncalma.tickets.backend.ticket.service.EscalamientoAreaService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints del representante de un área. No usan @PreAuthorize por rol: el servicio comprueba que el usuario
 * autenticado sea el representante del área (cualquier rol puede serlo).
 */
@RestController
@RequiredArgsConstructor
public class AreaRepresentanteController {

    private final EscalamientoAreaService escalamientoAreaService;

    public record RespuestaAreaRequest(String comentario) {}

    /** Áreas que representa el usuario actual (lista vacía si no es representante). */
    @GetMapping("/api/v1/areas/mis-areas")
    public ResponseEntity<List<AreaRepresentanteDTO.Area>> misAreas(Authentication authentication) {
        return ResponseEntity.ok(escalamientoAreaService.misAreas(authentication.getName()));
    }

    @GetMapping("/api/v1/tickets/mi-area")
    public ResponseEntity<List<AreaRepresentanteDTO.TicketEscalado>> escaladosAMiArea(Authentication authentication) {
        return ResponseEntity.ok(escalamientoAreaService.ticketsEscalados(authentication.getName()));
    }

    /** El representante responde el ticket escalado y este vuelve al técnico. */
    @PostMapping("/api/v1/tickets/{id}/respuesta-area")
    public ResponseEntity<Void> responder(
            @PathVariable UUID id, @RequestBody RespuestaAreaRequest request,
            Authentication authentication, HttpServletRequest http) {
        escalamientoAreaService.responder(id, request.comentario(), authentication.getName(), http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
