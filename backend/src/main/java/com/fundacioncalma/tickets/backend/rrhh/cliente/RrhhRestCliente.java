package com.fundacioncalma.tickets.backend.rrhh.cliente;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Cliente de solo lectura de la API de RR. HH. (contrato v1, ver docs/contrato-rrhh-v1.md):
 *   GET {url}/empleados/lookup?correo=...   -> un empleado (404 si no existe)
 *   GET {url}/empleados/{id}                -> un empleado (404 si no existe)
 *   GET {url}/empleados?q=...&limit=...     -> lista
 * Autenticación entre servicios con un token en el encabezado Authorization: Bearer.
 * Cualquier fallo (red, tiempo agotado, 401/403, 5xx, JSON inválido) se informa como RrhhNoDisponibleException,
 * para que quien llama decida si usa la copia local.
 */
public class RrhhRestCliente {

    private final String urlBase;
    private final String token;
    private final Duration tiempoMaximo;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public RrhhRestCliente(String urlBase, String token, int tiempoMaximoSegundos) {
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        this.token = token;
        this.tiempoMaximo = Duration.ofSeconds(Math.max(tiempoMaximoSegundos, 1));
        this.http = HttpClient.newBuilder().connectTimeout(tiempoMaximo).build();
    }

    public Optional<EmpleadoRrhh> porCorreo(String correo) {
        return uno("/empleados/lookup?correo=" + codificar(correo));
    }

    public Optional<EmpleadoRrhh> porId(UUID id) {
        return uno("/empleados/" + id);
    }

    public List<EmpleadoRrhh> buscar(String texto, int limite) {
        HttpResponse<String> respuesta = get("/empleados?q=" + codificar(texto == null ? "" : texto) + "&limit=" + limite);
        if (respuesta.statusCode() != 200) {
            throw new RrhhNoDisponibleException("RR. HH. respondió HTTP " + respuesta.statusCode() + " a la búsqueda");
        }
        try {
            return mapper.readValue(respuesta.body(), new TypeReference<List<EmpleadoRrhh>>() {});
        } catch (JsonProcessingException e) {
            throw new RrhhNoDisponibleException("RR. HH. entregó una lista de empleados inválida", e);
        }
    }

    public RrhhSnapshotPage snapshot(UUID afterId, Long hastaVersion, int limite) {
        StringBuilder ruta = new StringBuilder("/empleados/snapshot?limit=").append(limite);
        if (afterId != null) {
            ruta.append("&afterId=").append(afterId);
        }
        if (hastaVersion != null) {
            ruta.append("&hastaVersion=").append(hastaVersion);
        }
        HttpResponse<String> respuesta = get(ruta.toString());
        return leer(respuesta, RrhhSnapshotPage.class, "snapshot de empleados");
    }

    public RrhhCambiosPage cambios(long desde, int limite) {
        HttpResponse<String> respuesta = get("/empleados/cambios?desde=" + desde + "&limit=" + limite);
        return leer(respuesta, RrhhCambiosPage.class, "cambios de empleados");
    }

    private <T> T leer(HttpResponse<String> respuesta, Class<T> tipo, String recurso) {
        if (respuesta.statusCode() != 200) {
            throw new RrhhNoDisponibleException(
                    "RR. HH. respondió HTTP " + respuesta.statusCode() + " al consultar " + recurso);
        }
        try {
            return mapper.readValue(respuesta.body(), tipo);
        } catch (JsonProcessingException e) {
            throw new RrhhNoDisponibleException("RR. HH. entregó un " + recurso + " inválido", e);
        }
    }

    private Optional<EmpleadoRrhh> uno(String ruta) {
        HttpResponse<String> respuesta = get(ruta);
        if (respuesta.statusCode() == 404) {
            return Optional.empty();
        }
        if (respuesta.statusCode() != 200) {
            throw new RrhhNoDisponibleException("RR. HH. respondió HTTP " + respuesta.statusCode());
        }
        try {
            return Optional.of(mapper.readValue(respuesta.body(), EmpleadoRrhh.class));
        } catch (JsonProcessingException e) {
            throw new RrhhNoDisponibleException("RR. HH. entregó un empleado inválido", e);
        }
    }

    private HttpResponse<String> get(String ruta) {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlBase + ruta))
                .timeout(tiempoMaximo)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            return http.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RrhhNoDisponibleException("No se pudo conectar con RR. HH.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RrhhNoDisponibleException("La consulta a RR. HH. fue interrumpida", e);
        }
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}
