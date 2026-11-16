package com.fundacioncalma.tickets.backend.usuario.model;

import java.io.Serializable;
import java.util.UUID;

import com.fundacioncalma.tickets.backend.rrhh.model.Empleado;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuario", schema = "usuario")
public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado", unique = true)
    private Empleado empleado;

    @Column(name = "contrasena_hash", nullable = false, length = 255)
    private String contrasenaHash;

    // Una cuenta inactiva no puede iniciar sesión ni usar un token ya emitido
    @Builder.Default
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    // Contraseña temporal: hasta cambiarla, solo se permite el cambio de contraseña
    @Builder.Default
    @Column(name = "debe_cambiar_contrasena", nullable = false)
    private boolean debeCambiarContrasena = false;

    @Builder.Default
    @Column(name = "activada", nullable = false)
    private boolean activada = true;
}