package com.fundacioncalma.tickets.backend.ticket.controller;

import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fundacioncalma.tickets.backend.ticket.dto.CambiarEstadoRequest;
import com.fundacioncalma.tickets.backend.ticket.dto.EscalarTicketRequest;
import com.fundacioncalma.tickets.backend.ticket.dto.TicketResponseDTO; // <-- 1. IMPORTA EL NUEVO DTO
import com.fundacioncalma.tickets.backend.ticket.model.Ticket;
import com.fundacioncalma.tickets.backend.ticket.service.TicketService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    public record InformacionRequest(String comentario) {}

    @PostMapping
    public ResponseEntity<?> crearTicket(@RequestBody Map<String, Object> request, Authentication authentication) {
        String correoSolicitante = authentication.getName(); 
        
        String asunto = texto(request.get("asunto"));
        String descripcion = texto(request.get("descripcion"));
        Object categoriaCruda = request.get("categoriaId");

        if (asunto == null || descripcion == null || categoriaCruda == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asunto, descripción y categoría son obligatorios");
        }
        if (asunto.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El asunto no puede superar los 200 caracteres");
        }
        UUID categoriaId;
        try {
            categoriaId = UUID.fromString(categoriaCruda.toString());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoría inválida");
        }

        // La prioridad ya no la envía el solicitante: la define el sistema según la categoría
        Ticket nuevoTicket = ticketService.crearTicket(correoSolicitante, asunto, descripcion, categoriaId);
        
        return ResponseEntity.status(201).body(Map.of(
                "mensaje", "Ticket creado exitosamente",
                "ticketId", nuevoTicket.getId(),
                "codigo", nuevoTicket.getCodigo(),
                "prioridad", nuevoTicket.getPrioridad().getNombre()
        ));
    }

    private static String texto(Object valor) {
        return (valor instanceof String s && !s.isBlank()) ? s.trim() : null;
    }

    @PostMapping("/{id}/escalar")
    @PreAuthorize("hasAnyRole('Técnico TI', 'Supervisor', 'Administrador', 'Superadmin')")
    public ResponseEntity<?> escalarTicket(
        @PathVariable UUID id,
        @RequestBody EscalarTicketRequest request,
        Authentication authentication,
        HttpServletRequest httpRequest) {
    
        String usuarioNombre = (authentication != null && authentication.getName() != null)
                ? authentication.getName()
                : "Sistema";
        String ipOrigen = httpRequest.getRemoteAddr();

        // <-- 2. CAMBIA TICKET POR TICKETRESPONSEDTO
        TicketResponseDTO ticketEscalado = ticketService.escalarTicket(id, request, usuarioNombre, ipOrigen);
        
        return ResponseEntity.ok(Map.of(
                // <-- 3. AHORA ACCEDES AL DTO (que ya es un String), NO A LA ENTIDAD
                "mensaje", "Ticket escalado exitosamente al área: " + ticketEscalado.getAreaEscalada(),
                "ticket", ticketEscalado // <-- 4. ESTO AHORA ES SEGURO. Jackson serializará el DTO plano sin errores
        ));
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('Técnico TI', 'Supervisor', 'Administrador', 'Superadmin')")
    public ResponseEntity<?> actualizarEstado(
            @PathVariable UUID id,
            @RequestBody CambiarEstadoRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        // Extraemos el usuario autenticado desde el token JWT
        String usuarioNombre = (authentication != null && authentication.getName() != null) 
                ? authentication.getName() 
                : "Sistema";
        
        // Obtenemos la IP del solicitante
        String ipOrigen = httpRequest.getHeader("X-Forwarded-For");
        if (ipOrigen == null || ipOrigen.isEmpty()) {
            ipOrigen = httpRequest.getRemoteAddr();
        }

        Ticket ticketActualizado = ticketService.actualizarEstado(id, request, usuarioNombre, ipOrigen);
        
        // Retornamos campos planos en lugar del objeto Ticket entidad
        return ResponseEntity.ok(Map.of(
                "mensaje", "Estado del ticket actualizado exitosamente",
                "ticketId", ticketActualizado.getId(),
                "codigo", ticketActualizado.getCodigo()
        ));
    }

    @PostMapping("/{id}/informacion")
    public ResponseEntity<?> responderSolicitudInformacion(
            @PathVariable UUID id,
            @RequestBody InformacionRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        Ticket ticket = ticketService.responderSolicitudInformacion(
                id,
                request.comentario(),
                authentication.getName(),
                httpRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Información enviada y ticket devuelto a revisión",
                "ticketId", ticket.getId(),
                "codigo", ticket.getCodigo()));
    }


    @GetMapping("/bandeja")
@PreAuthorize("hasAnyRole('Técnico TI', 'Supervisor', 'Administrador', 'Superadmin')")
public ResponseEntity<?> obtenerBandeja(Authentication authentication) {
    return ResponseEntity.ok(ticketService.obtenerBandejaTecnico(authentication));
}
}