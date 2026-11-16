package com.fundacioncalma.tickets.backend.usuario.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Contraseñas temporales de 12 caracteres: 3 mayúsculas, 3 minúsculas, 3 dígitos y 3 más, mezclados.
 * Se excluyen los caracteres que se confunden al leerlos (I, l, 1, O, o, 0). Siempre cumplen PoliticaContrasena.
 */
public final class GeneradorContrasena {

    private static final String MAYUSCULAS = "ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghjkmnpqrstuvwxyz";
    private static final String DIGITOS = "23456789";
    private static final String TODOS = MAYUSCULAS + MINUSCULAS + DIGITOS;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private GeneradorContrasena() {}

    public static String generar() {
        return generar(ALEATORIO);
    }

    static String generar(Random aleatorio) {
        List<Character> caracteres = new ArrayList<>();
        agregar(caracteres, MAYUSCULAS, 3, aleatorio);
        agregar(caracteres, MINUSCULAS, 3, aleatorio);
        agregar(caracteres, DIGITOS, 3, aleatorio);
        agregar(caracteres, TODOS, 3, aleatorio);
        Collections.shuffle(caracteres, aleatorio);
        StringBuilder resultado = new StringBuilder();
        caracteres.forEach(resultado::append);
        return resultado.toString();
    }

    private static void agregar(List<Character> destino, String alfabeto, int cantidad, Random aleatorio) {
        for (int i = 0; i < cantidad; i++) {
            destino.add(alfabeto.charAt(aleatorio.nextInt(alfabeto.length())));
        }
    }
}
