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

/** Un inicio de sesión (exitoso o fallido). Tabla de solo inserción. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "registro_acceso", schema = "usuario")
public class RegistroAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Nulo si el correo ingresado no corresponde a ningún usuario
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "correo_intentado", nullable = false, length = 255)
    private String correoIntentado;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "ip_origen", length = 50)
    private String ipOrigen;

    @Column(name = "resultado", nullable = false, length = 30)
    private String resultado;
}
