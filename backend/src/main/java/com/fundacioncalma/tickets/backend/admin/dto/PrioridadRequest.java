package com.fundacioncalma.tickets.backend.admin.dto;
import lombok.Data;
@Data
public class PrioridadRequest {
    private String nombre;
    private Integer slaHoras; // RF-32
}