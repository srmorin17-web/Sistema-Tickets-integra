package com.fundacioncalma.tickets.backend.admin.dto;
import lombok.Data;
import java.util.UUID;
@Data
public class AreaEscalamientoRequest {
    private String nombre;
    private String descripcion;
    private UUID contactoResponsableId;
    private Boolean activo;
}