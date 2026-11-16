package com.fundacioncalma.tickets.backend.usuario.service;

import java.time.LocalDateTime;
import java.time.Instant;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fundacioncalma.tickets.backend.seguridad.repository.RegistroAccesoRepository;
import com.fundacioncalma.tickets.backend.seguridad.service.AuditoriaAccesoService;
import com.fundacioncalma.tickets.backend.seguridad.service.AuditoriaUsuarioService;
import com.fundacioncalma.tickets.backend.shared.security.JwtService;
import com.fundacioncalma.tickets.backend.usuario.dto.AuthRequest;
import com.fundacioncalma.tickets.backend.usuario.dto.AuthResponse;
import com.fundacioncalma.tickets.backend.usuario.model.RefreshSession;
import com.fundacioncalma.tickets.backend.usuario.model.Usuario;
import com.fundacioncalma.tickets.backend.usuario.repository.RefreshSessionRepository;
import com.fundacioncalma.tickets.backend.usuario.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditoriaAccesoService auditoriaAccesoService;
    private final AuditoriaUsuarioService auditoriaUsuarioService;
    private final RegistroAccesoRepository registroAccesoRepository;
    private final RefreshSessionRepository refreshSessionRepository;

    /** Intentos fallidos seguidos que bloquean temporalmente el inicio de sesión de un correo. */
    @Value("${seguridad.login.max-intentos:5}")
    private int maxIntentos;

    /** Minutos que dura el bloqueo (los intentos fallidos se cuentan en esta ventana). */
    @Value("${seguridad.login.ventana-minutos:15}")
    private int ventanaMinutos;

    @Value("${auth.refresh-token.idle-timeout-ms:28800000}")
    private long refreshIdleTimeoutMs;

    @Value("${auth.refresh-token.absolute-timeout-ms:604800000}")
    private long refreshAbsoluteTimeoutMs;

    @Value("${rrhh.estado-activo:ACTIVO}")
    private String estadoEmpleadoActivo;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Hash BCrypt válido, usado solo para mantener el tiempo de respuesta constante cuando el correo no existe.
    private static final String HASH_FICTICIO = "$2b$10$QvUevqh7RnO/YGK/zVC4SuxCIkp2kbetTVq8SLdESoiyYId99koMa";

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public LoginResult login(AuthRequest request, String ip) {
        String correo = request.getCorreo() == null ? "" : request.getCorreo().trim();
        String contrasena = request.getContrasena() == null ? "" : request.getContrasena();

        // Bloqueo temporal tras demasiados intentos fallidos. Los intentos durante el bloqueo se registran como
        // BLOQUEADO (no cuentan como fallidos), de modo que el bloqueo no se prolonga indefinidamente.
        if (!correo.isEmpty() && estaBloqueado(correo)) {
            registrarAcceso(correo, ip, "BLOQUEADO");
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos fallidos");
        }

        Usuario usuario = correo.isEmpty() ? null : usuarioRepository.findByCorreoCorporativo(correo).orElse(null);

        // Se compara siempre contra un hash para que el tiempo de respuesta no revele si el correo existe
        boolean contrasenaCorrecta = passwordEncoder.matches(contrasena,
                usuario != null ? usuario.getContrasenaHash() : HASH_FICTICIO);
        // Una cuenta inactiva se trata igual que credenciales incorrectas: no se revela que existe
        boolean valido = usuario != null && usuario.isActivo() && usuario.isActivada()
                && empleadoActivo(usuario) && contrasenaCorrecta;

        registrarAcceso(correo, ip, valido ? "EXITOSO" : "FALLIDO");

        if (!valido) {
            // Mensaje único: no se distingue entre correo inexistente, cuenta inactiva y contraseña incorrecta
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        Instant now = Instant.now();
        refreshSessionRepository.deleteExpired(now);
        String refreshToken = generarRefreshToken();
        RefreshSession session = new RefreshSession();
        session.setUsuario(usuario);
        session.setRefreshTokenHash(hashRefreshToken(refreshToken));
        session.setFamilyId(UUID.randomUUID());
        session.setCreatedAt(now);
        session.setLastActivityAt(now);
        session.setAbsoluteExpiresAt(now.plusMillis(refreshAbsoluteTimeoutMs));
        refreshSessionRepository.save(session);

        String token = jwtService.generateToken(usuario.getEmpleado().getCorreoCorporativo(), usuario.getRol().getNombre());
        AuthResponse response = new AuthResponse(token, usuario.getRol().getNombre(),
                usuario.getEmpleado().getNombres() + " " + usuario.getEmpleado().getApellidos(),
                usuario.isDebeCambiarContrasena());
        return new LoginResult(response, refreshToken, session.getAbsoluteExpiresAt());
    }

    @Transactional
    public Optional<RefreshResult> renovarSesion(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }

        Optional<RefreshSession> encontrada = refreshSessionRepository
                .findByRefreshTokenHashForUpdate(hashRefreshToken(refreshToken));
        if (encontrada.isEmpty()) {
            return Optional.empty();
        }

        RefreshSession anterior = encontrada.get();
        Instant now = Instant.now();
        boolean inactiva = anterior.getLastActivityAt().plusMillis(refreshIdleTimeoutMs).isBefore(now);
        boolean vencida = !anterior.getAbsoluteExpiresAt().isAfter(now);
        if (anterior.isRevoked() || !anterior.getUsuario().isActivo() || !anterior.getUsuario().isActivada()
                || !empleadoActivo(anterior.getUsuario()) || inactiva || vencida) {
            refreshSessionRepository.revokeFamily(anterior.getFamilyId());
            return Optional.empty();
        }

        anterior.setRevoked(true);
        refreshSessionRepository.save(anterior);

        String nuevoRefreshToken = generarRefreshToken();
        RefreshSession siguiente = new RefreshSession();
        siguiente.setUsuario(anterior.getUsuario());
        siguiente.setRefreshTokenHash(hashRefreshToken(nuevoRefreshToken));
        siguiente.setFamilyId(anterior.getFamilyId());
        siguiente.setCreatedAt(now);
        siguiente.setLastActivityAt(now);
        siguiente.setAbsoluteExpiresAt(anterior.getAbsoluteExpiresAt());
        refreshSessionRepository.save(siguiente);

        Usuario usuario = anterior.getUsuario();
        String accessToken = jwtService.generateToken(
                usuario.getEmpleado().getCorreoCorporativo(), usuario.getRol().getNombre());
        return Optional.of(new RefreshResult(accessToken, nuevoRefreshToken, anterior.getAbsoluteExpiresAt()));
    }

    @Transactional
    public void cerrarSesion(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshSessionRepository.findByRefreshTokenHashForUpdate(hashRefreshToken(refreshToken))
                .ifPresent(session -> refreshSessionRepository.revokeFamily(session.getFamilyId()));
    }

    /**
     * El usuario cambia su propia contraseña. Es lo único que puede hacer mientras tiene una contraseña temporal.
     * Una contraseña actual incorrecta cuenta como intento fallido (impide adivinarla con un token robado).
     */
    @Transactional
    public void cambiarContrasena(String correo, String actual, String nueva, String ip) {
        Usuario usuario = usuarioRepository.findByCorreoCorporativo(correo)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta no disponible"));

        if (estaBloqueado(correo)) {
            registrarAcceso(correo, ip, "BLOQUEADO");
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos fallidos");
        }
        if (actual == null || !passwordEncoder.matches(actual, usuario.getContrasenaHash())) {
            registrarAcceso(correo, ip, "FALLIDO");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La contraseña actual no es correcta");
        }
        Optional<String> motivo = PoliticaContrasena.validar(nueva, actual, correo);
        if (motivo.isPresent()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, motivo.get());
        }

        usuario.setContrasenaHash(passwordEncoder.encode(nueva));
        usuario.setDebeCambiarContrasena(false);
        usuarioRepository.save(usuario);
        refreshSessionRepository.revokeAllForUser(usuario.getId());
        auditoriaUsuarioService.registrar(usuario, usuario, "CAMBIO_CONTRASENA", null, ip);
    }

    private String generarRefreshToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashRefreshToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }

    private boolean empleadoActivo(Usuario usuario) {
        String estadoLaboral = usuario.getEmpleado().getEstadoLaboral();
        return estadoLaboral != null && estadoEmpleadoActivo.equalsIgnoreCase(estadoLaboral.trim());
    }

    public record LoginResult(AuthResponse response, String refreshToken, Instant absoluteExpiresAt) {}
    public record RefreshResult(String accessToken, String refreshToken, Instant absoluteExpiresAt) {}

    /** Intentos fallidos desde el último acceso exitoso (o desde el inicio de la ventana, lo más reciente). */
    private boolean estaBloqueado(String correo) {
        LocalDateTime inicioVentana = LocalDateTime.now().minusMinutes(ventanaMinutos);
        LocalDateTime ultimoExito = registroAccesoRepository.findUltimoExitoso(correo);
        LocalDateTime desde = (ultimoExito != null && ultimoExito.isAfter(inicioVentana)) ? ultimoExito : inicioVentana;
        return registroAccesoRepository.contarFallidosDesde(correo, desde) >= maxIntentos;
    }

    /** Registra IP, fecha, hora y usuario de cada inicio de sesión. Un fallo del registro no bloquea el acceso, pero queda en el log. */
    private void registrarAcceso(String correo, String ip, String resultado) {
        try {
            auditoriaAccesoService.registrarLogin(correo, ip, resultado);
        } catch (Exception e) {
            log.error("❌ No se pudo registrar el inicio de sesión de {}: {}", correo, e.getMessage());
        }
    }
}
