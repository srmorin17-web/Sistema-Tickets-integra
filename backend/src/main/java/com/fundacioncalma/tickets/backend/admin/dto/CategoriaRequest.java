package com.fundacioncalma.tickets.backend.admin.dto;
import java.util.UUID;
import lombok.Data;
@Data
public class CategoriaRequest {
    private String nombre;
    private String descripcion;
    private Boolean activo;
    // Prioridad que el sistema asigna a los tickets de la categoría (obligatoria al crear; opcional al editar)
    private UUID prioridadDefectoId;
}