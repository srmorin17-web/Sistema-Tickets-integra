package com.fundacioncalma.tickets.backend.ticket.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fundacioncalma.tickets.backend.usuario.model.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "historial_auditoria", schema = "ticket")
public class HistorialAuditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    // Quién ejecutó la acción (columna usuario_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // Nota: Usamos String para los JSONB para evitar complejidades de mapeo de Hibernate 6.
    // El servicio se encargará de serializar el Map a String JSON.
    @Column(name = "tipo_accion", nullable = false, length = 100)
    private String tipoAccion;

    @Column(name = "estado_anterior_id")
    private UUID estadoAnteriorId;

    @Column(name = "estado_nuevo_id")
    private UUID estadoNuevoId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_anteriores", columnDefinition = "jsonb")
    private String datosAnteriores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_nuevos", columnDefinition = "jsonb")
    private String datosNuevos;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "ip_origen", length = 50)
    private String ipOrigen;
}
