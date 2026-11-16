package com.fundacioncalma.tickets.backend.ticket.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class CambiarEstadoRequest {
    private UUID nuevoEstadoId;
    private String comentario; // Obligatorio si el nuevo estado es "Resuelto" o "Cancelado"
    private boolean esInterno; // Si el comentario es solo visible para técnicos
}