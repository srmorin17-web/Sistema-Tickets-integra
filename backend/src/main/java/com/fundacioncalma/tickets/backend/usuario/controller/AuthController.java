package com.fundacioncalma.tickets.backend.usuario.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fundacioncalma.tickets.backend.usuario.dto.AuthRequest;
import com.fundacioncalma.tickets.backend.usuario.dto.AuthResponse;
import com.fundacioncalma.tickets.backend.usuario.service.AuthService;
import com.fundacioncalma.tickets.backend.usuario.service.CuentaActivacionService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final String COOKIE_NAME = "refresh_token";
    private static final String REFRESH_HEADER = "X-Auth-Refresh";

    private final AuthService authService;
    private final CuentaActivacionService cuentaActivacionService;

    @Value("${auth.refresh-cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${auth.refresh-cookie.same-site:Lax}")
    private String cookieSameSite;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        AuthService.LoginResult result = authService.login(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(result.refreshToken(), result.absoluteExpiresAt()).toString())
                .body(result.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(HttpServletRequest request) {
        validarPeticionDeCookie(request);
        Optional<AuthService.RefreshResult> result = authService.renovarSesion(leerRefreshToken(request));
        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, cookie(null, null).toString())
                    .build();
        }
        AuthService.RefreshResult tokens = result.get();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(tokens.refreshToken(), tokens.absoluteExpiresAt()).toString())
                .body(new TokenResponse(tokens.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        validarPeticionDeCookie(request);
        authService.cerrarSesion(leerRefreshToken(request));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie(null, null).toString())
                .build();
    }

    private ResponseCookie cookie(String value, Instant absoluteExpiresAt) {
        Duration maxAge = absoluteExpiresAt == null
                ? Duration.ZERO
                : Duration.between(Instant.now(), absoluteExpiresAt);
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(COOKIE_NAME, value == null ? "" : value)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth")
                .sameSite(cookieSameSite)
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge);
        return builder.build();
    }

    private String leerRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void validarPeticionDeCookie(HttpServletRequest request) {
        if (!"1".equals(request.getHeader(REFRESH_HEADER))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solicitud de renovación no válida");
        }
    }

    public record TokenResponse(String token) {}

    public record ActivarCuentaRequest(String token, String contrasenaNueva) {}

    @PostMapping("/activar-cuenta")
    public ResponseEntity<Void> activarCuenta(@RequestBody ActivarCuentaRequest request) {
        cuentaActivacionService.activar(request.token(), request.contrasenaNueva());
        return ResponseEntity.noContent().build();
    }
}