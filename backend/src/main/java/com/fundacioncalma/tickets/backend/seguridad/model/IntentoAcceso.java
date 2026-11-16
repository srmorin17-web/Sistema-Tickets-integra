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

/** Un intento de acceso a una ruta o vista no permitida. Tabla de solo inserción. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "intento_acceso", schema = "usuario")
public class IntentoAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "ruta_intentada", nullable = false, length = 500)
    private String rutaIntentada;

    @Column(name = "metodo_http", nullable = false, length = 20)
    private String metodoHttp;

    @Column(name = "fecha_intento")
    private LocalDateTime fechaIntento;

    @Column(name = "ip_origen", length = 50)
    private String ipOrigen;

    // DENEGADO (API) | VISTA_DENEGADA (pantalla del frontend)
    @Column(name = "resultado", nullable = false, length = 50)
    private String resultado;
}
