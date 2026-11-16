package com.fundacioncalma.tickets.backend.usuario.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fundacioncalma.tickets.backend.notification.service.EmailService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CuentaActivacionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:5174}")
    private String frontendUrl;

    @Value("${auth.activation-token.expiration-hours:24}")
    private long expiracionHoras;

    @Transactional
    public void enviarEnlace(UUID usuarioId) {
        Cuenta cuenta = jdbcTemplate.query(
                """
                SELECT u.activada, e.correo_corporativo, e.nombres
                FROM usuario.usuario u
                JOIN empleado.empleado e ON e.id = u.id_empleado
                WHERE u.id = ?
                FOR UPDATE OF u
                """,
                rs -> rs.next()
                        ? new Cuenta(rs.getBoolean("activada"), rs.getString("correo_corporativo"), rs.getString("nombres"))
                        : null,
                usuarioId);
        if (cuenta == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada");
        }
        if (cuenta.activada()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta ya fue activada");
        }

        jdbcTemplate.update(
                """
                DELETE FROM usuario.activacion_token
                WHERE id_usuario = ?
                """,
                usuarioId);

        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String tokenHash = hash(token);
        long horas = Math.max(1, expiracionHoras);
        Instant vence = Instant.now().plus(Duration.ofHours(horas));

        jdbcTemplate.update(
                """
                INSERT INTO usuario.activacion_token (token_hash, id_usuario, vence_en)
                VALUES (?, ?, ?)
                """,
                tokenHash,
                usuarioId,
                OffsetDateTime.ofInstant(vence, ZoneOffset.UTC));

        String baseUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
        String enlace = baseUrl + "/activar-cuenta?token=" + token;
        emailService.enviarEnlaceActivacionCuenta(cuenta.correo(), cuenta.nombres(), enlace, horas);
    }

    @Transactional
    public void activar(String token, String nuevaContrasena) {
        if (token == null || token.isBlank()) {
            throw enlaceInvalido();
        }
        String tokenHash = hash(token);
        CuentaPendiente pendiente = jdbcTemplate.query(
                """
                SELECT u.id, e.correo_corporativo
                FROM usuario.activacion_token t
                JOIN usuario.usuario u ON u.id = t.id_usuario
                JOIN empleado.empleado e ON e.id = u.id_empleado
                WHERE t.token_hash = ? AND t.consumido_en IS NULL AND t.vence_en > CURRENT_TIMESTAMP
                FOR UPDATE OF t, u
                """,
                rs -> rs.next() ? new CuentaPendiente(rs.getObject("id", UUID.class), rs.getString("correo_corporativo")) : null,
                tokenHash);
        if (pendiente == null) {
            throw enlaceInvalido();
        }

        PoliticaContrasena.validar(nuevaContrasena, null, pendiente.correo())
                .ifPresent(motivo -> {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, motivo);
                });

        jdbcTemplate.update(
                """
                UPDATE usuario.usuario
                SET contrasena_hash = ?, activada = TRUE, debe_cambiar_contrasena = FALSE
                WHERE id = ?
                """,
                passwordEncoder.encode(nuevaContrasena),
                pendiente.id());
        jdbcTemplate.update(
                """
                DELETE FROM usuario.activacion_token
                WHERE id_usuario = ?
                """,
                pendiente.id());
    }

    private ResponseStatusException enlaceInvalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace de activación es inválido o venció");
    }

    private String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No está disponible SHA-256", e);
        }
    }

    private record Cuenta(boolean activada, String correo, String nombres) {}

    private record CuentaPendiente(UUID id, String correo) {}
}
