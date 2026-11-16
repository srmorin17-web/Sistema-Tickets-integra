package com.fundacioncalma.tickets.backend.ticket.model;

import com.fundacioncalma.tickets.backend.rrhh.model.Empleado;
import com.fundacioncalma.tickets.backend.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity

@Table(name = "ticket", schema = "ticket")
public class Ticket implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String asunto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Empleado solicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_asignado_id")
    private Usuario tecnicoAsignado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria; // Asumimos que creas esta entidad simple similar a Rol

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prioridad_id", nullable = false)
    private Prioridad prioridad; // Asumimos entidad simple con campo 'slaHoras'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_vencimiento_sla")
    private LocalDateTime fechaVencimientoSla;

    @Column(name = "pausado")
    private Boolean pausado = false;

    @Column(name = "fecha_pausa_sla")
    private LocalDateTime fechaPausaSla;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre; // Mapea la columna fecha_cierre de la BD

    @Column(name = "comentario_cierre", columnDefinition = "TEXT")
    private String comentarioCierre; // Mapea el comentario final (RNF-09 Auditoría)

        @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_escalada_id")
    private AreaEscalamiento areaEscalada; 

    @Column(name = "motivo_escalamiento", columnDefinition = "TEXT")
    private String motivoEscalamiento;
    
    @Column(name = "sla_horas_snapshot")
private Integer slaHorasSnapshot;
 
    
}

