package com.fundacioncalma.tickets.backend.ticket.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "area_escalamiento", schema = "ticket")
public class AreaEscalamiento {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    
    @Column(unique = true, nullable = false, length = 150) private String nombre;
    @Column(length = 500) private String descripcion;
    
    @Column(name = "contacto_responsable_id")
    private UUID contactoResponsableId; // ID del empleado líder del área
    
    @Builder.Default
    @Column(nullable = false) private Boolean activo = true;
}