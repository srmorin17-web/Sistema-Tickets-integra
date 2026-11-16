package com.fundacioncalma.tickets.backend.usuario.service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/**
 * Política de contraseñas del sistema de tickets (el frontend aplica la misma antes de enviar):
 * 10 a 72 caracteres (BCrypt solo usa los primeros 72 bytes), al menos una letra y un número,
 * distinta de la actual, sin contener el nombre de usuario y fuera de una lista de contraseñas conocidas.
 * La contraseña de la cuenta semilla (publicada en el repositorio) está prohibida.
 */
public final class PoliticaContrasena {

    public static final int MINIMO = 10;
    public static final int MAXIMO_BYTES = 72;

    private static final Set<String> PROHIBIDAS = Set.of(
            "admin123!", "admin123", "password1", "password123", "contraseña1", "contraseña123",
            "1234567890a", "qwerty12345", "calma12345", "calma2026!");

    private PoliticaContrasena() {}

    /** Devuelve el motivo del rechazo, o vacío si la contraseña es aceptable. */
    public static Optional<String> validar(String nueva, String actual, String correo) {
        if (nueva == null || nueva.isEmpty()) {
            return Optional.of("La contraseña nueva es obligatoria");
        }
        if (nueva.length() < MINIMO) {
            return Optional.of("Debe tener al menos " + MINIMO + " caracteres");
        }
        if (nueva.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES) {
            return Optional.of("Es demasiado larga (máximo " + MAXIMO_BYTES + " caracteres)");
        }
        boolean hayLetra = nueva.chars().anyMatch(Character::isLetter);
        boolean hayDigito = nueva.chars().anyMatch(Character::isDigit);
        if (!hayLetra || !hayDigito) {
            return Optional.of("Debe incluir al menos una letra y un número");
        }
        if (nueva.equals(actual)) {
            return Optional.of("Debe ser distinta de la contraseña actual");
        }
        if (PROHIBIDAS.contains(nueva.toLowerCase())) {
            return Optional.of("Es una contraseña demasiado común");
        }
        if (correo != null && correo.contains("@")) {
            String usuario = correo.substring(0, correo.indexOf('@')).toLowerCase();
            if (usuario.length() >= 3 && nueva.toLowerCase().contains(usuario)) {
                return Optional.of("No debe contener su nombre de usuario");
            }
        }
        return Optional.empty();
    }
}
