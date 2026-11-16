package com.fundacioncalma.tickets.backend.usuario.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sesion", schema = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class RefreshSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "refresh_token", nullable = false, unique = true, columnDefinition = "text")
    private String refreshTokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant createdAt;

    @Column(name = "fecha_ultima_actividad", nullable = false)
    private Instant lastActivityAt;

    @Column(name = "fecha_expiracion", nullable = false)
    private Instant absoluteExpiresAt;

    @Column(name = "revocada", nullable = false)
    private boolean revoked;
}
