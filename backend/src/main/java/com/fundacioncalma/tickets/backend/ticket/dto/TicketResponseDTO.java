package com.fundacioncalma.tickets.backend.ticket.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TicketResponseDTO {
   private UUID id;
    private String codigo;
    private String asunto;
    private String estado;
    private String categoria;
    private String prioridad;
    private String areaEscalada;
    private LocalDateTime fechaCreacion;
    // Agrega aquí cualquier otro campo que necesites devolver al Frontend / PowerShell 
}
