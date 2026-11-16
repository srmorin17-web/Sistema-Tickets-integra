package com.fundacioncalma.tickets.backend.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BandejaTicketDTO {
    private UUID id;
    private String codigo;
    private String asunto;
    private String solicitante;
    private String prioridad;
    private String estado;
    private LocalDateTime fechaVencimientoSla;
    private Integer slaHorasSnapshot; // Necesario para calcular el % en el frontend
    private Boolean pausado;
}