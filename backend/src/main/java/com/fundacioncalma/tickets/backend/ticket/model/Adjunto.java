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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Archivo adjunto de un ticket. En la base solo se guardan los metadatos y la URL; el archivo vive en el almacenamiento. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "adjunto", schema = "ticket")
public class Adjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    // URL en el proveedor (Cloudinary) o ruta local. Vacía si el archivo fue purgado por la política de retención.
    @Column(name = "ruta_cloudinary", nullable = false, length = 500)
    private String rutaCloudinary;

    // Identificador con el que se descarga y se elimina el archivo en el proveedor
    @Column(name = "public_id", nullable = false, length = 300)
    private String publicId;

    @Column(name = "tipo_mime", nullable = false, length = 100)
    private String tipoMime;

    @Column(name = "tamano_bytes", nullable = false)
    private Integer tamanoBytes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subido_por_id")
    private Usuario subidoPor;

    @Column(name = "fecha_subida")
    private LocalDateTime fechaSubida;

    @Column(name = "purgado", nullable = false)
    private boolean purgado;

    @Column(name = "fecha_purgado")
    private LocalDateTime fechaPurgado;
}
