package com.fundacioncalma.tickets.backend.ticket.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class EscalarTicketRequest {
    private UUID areaEscalamientoId;
    private String motivo;
}