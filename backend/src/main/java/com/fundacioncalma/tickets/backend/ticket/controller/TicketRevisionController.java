package com.fundacioncalma.tickets.backend.ticket.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fundacioncalma.tickets.backend.ticket.service.TicketRevisionService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TicketRevisionController {

    private final TicketRevisionService revisionService;

    public record RevisionRequest(String comentario) {}
    public record ReaperturaRequest(String motivo) {}

    /** El solicitante confirma la solución y cierra el ticket inmediatamente. */
    @PostMapping("/api/v1/tickets/{id}/confirmar-cierre")
    public ResponseEntity<Void> confirmarCierre(
            @PathVariable UUID id, Authentication authentication, HttpServletRequest http) {
        revisionService.confirmarSolucion(id, authentication.getName(), http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    /** El solicitante deja su comentario sobre un ticket ya resuelto (opcional, una sola vez). */
    @PostMapping("/api/v1/tickets/{id}/revision")
    public ResponseEntity<Void> registrarRevision(
            @PathVariable UUID id, @RequestBody RevisionRequest request,
            Authentication authentication, HttpServletRequest http) {
        revisionService.registrarRevision(id, request.comentario(), authentication.getName(), http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    /** El solicitante reabre un ticket resuelto que no solucionó el problema. */
    @PostMapping("/api/v1/tickets/{id}/reabrir")
    public ResponseEntity<Void> solicitarReapertura(
            @PathVariable UUID id, @RequestBody ReaperturaRequest request,
            Authentication authentication, HttpServletRequest http) {
        revisionService.solicitarReapertura(id, request.motivo(), authentication.getName(), http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
