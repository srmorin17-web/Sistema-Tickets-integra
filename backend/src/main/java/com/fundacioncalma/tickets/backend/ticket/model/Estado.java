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
@Table(name = "estado", schema = "ticket")
public class Estado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "color_hex", nullable = false, length = 7)
    private String colorHex;

    @Column(nullable = false)
    private Integer orden;

    @Builder.Default
    @Column(name = "es_estado_final")
    private Boolean esEstadoFinal = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;
}