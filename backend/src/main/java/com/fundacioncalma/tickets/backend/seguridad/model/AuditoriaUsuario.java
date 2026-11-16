package com.fundacioncalma.tickets.backend.seguridad.model;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Acción sobre una cuenta de usuario (creación, cambio de rol, activación, contraseñas). Tabla de solo inserción; nunca guarda contraseñas. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auditoria_usuario", schema = "usuario")
public class AuditoriaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_actor")
    private Usuario actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_afectado")
    private Usuario afectado;

    @Column(name = "accion", nullable = false, length = 40)
    private String accion;

    @Column(name = "detalle", length = 500)
    private String detalle;

    @Column(name = "ip_origen", length = 50)
    private String ipOrigen;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;
}
