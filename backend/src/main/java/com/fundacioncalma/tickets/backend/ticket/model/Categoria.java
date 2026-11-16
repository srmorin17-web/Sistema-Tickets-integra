package com.fundacioncalma.tickets.backend.ticket.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "categoria", schema = "ticket")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    // Prioridad que el sistema asigna a los tickets de esta categoría
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prioridad_defecto_id")
    private Prioridad prioridadDefecto;
}
