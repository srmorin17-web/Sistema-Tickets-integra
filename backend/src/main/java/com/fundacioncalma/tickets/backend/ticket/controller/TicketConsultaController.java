package com.fundacioncalma.tickets.backend.ticket.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fundacioncalma.tickets.backend.ticket.dto.TicketDetalleDTO;
import com.fundacioncalma.tickets.backend.ticket.service.TicketConsultaService;

import lombok.RequiredArgsConstructor;

/**
 * Endpoints de lectura de tickets. Comparte la ruta base con TicketController;
 * Spring resuelve primero las rutas literales (/bandeja, /mis-tickets) y luego /{id}.
 */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketConsultaController {

    private final TicketConsultaService ticketConsultaService;

    /** Tickets creados por el usuario autenticado, del más reciente al más antiguo. */
    @GetMapping("/mis-tickets")
    public ResponseEntity<List<TicketDetalleDTO>> misTickets(Authentication authentication) {
        return ResponseEntity.ok(ticketConsultaService.misTickets(authentication.getName()));
    }

    /** Listado global de tickets para administración; el historial completo se carga al abrir el detalle. */
    @GetMapping("/administracion")
    @PreAuthorize("hasAnyRole('Administrador', 'Superadmin')")
    public ResponseEntity<List<TicketDetalleDTO>> todosLosTickets() {
        return ResponseEntity.ok(ticketConsultaService.todosLosTickets());
    }

    /** Detalle con historial. El personal de TI ve cualquier ticket; los demás, solo los propios. */
    @GetMapping("/{id}")
    public ResponseEntity<TicketDetalleDTO> detalle(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ticketConsultaService.detalle(id, authentication));
    }
}
