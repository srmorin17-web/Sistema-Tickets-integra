package com.fundacioncalma.tickets.backend.almacenamiento;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Almacenamiento en Cloudinary (almacenamiento.proveedor=cloudinary), usando su API REST firmada (sin SDK).
 *
 * Los archivos se suben como "raw" con type=authenticated: NO son públicos. El navegador nunca recibe la URL de
 * Cloudinary; el backend comprueba los permisos del usuario y descarga el archivo con una petición firmada.
 * Firma: SHA-1 en hexadecimal de los parámetros ordenados alfabéticamente ("a=1&b=2") seguidos del api_secret,
 * sin incluir file, cloud_name, resource_type ni api_key.
 */
@Component
@ConditionalOnProperty(name = "almacenamiento.proveedor", havingValue = "cloudinary")
public class CloudinaryAlmacenamiento implements AlmacenamientoArchivos {

    private static final String TIPO_ENTREGA = "authenticated";
    private static final Pattern PUBLIC_ID = Pattern.compile("\"public_id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SECURE_URL = Pattern.compile("\"secure_url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern RESULTADO = Pattern.compile("\"result\"\\s*:\\s*\"([^\"]+)\"");

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String carpeta;
    private final String urlBase;
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public CloudinaryAlmacenamiento(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret,
            @Value("${cloudinary.carpeta:calma-tickets}") String carpeta,
            @Value("${cloudinary.api-base-url:https://api.cloudinary.com/v1_1}") String urlBase) {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            // Falla al arrancar en lugar de al primer adjunto
            throw new IllegalStateException(
                    "almacenamiento.proveedor=cloudinary requiere CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY y CLOUDINARY_API_SECRET");
        }
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.carpeta = carpeta;
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    }

    @Override
    public ArchivoAlmacenado guardar(String clave, String tipoMime, byte[] contenido) {
        String publicId = carpeta + "/" + clave;
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        Map<String, String> firmados = new TreeMap<>();
        firmados.put("public_id", publicId);
        firmados.put("timestamp", timestamp);
        firmados.put("type", TIPO_ENTREGA);

        String limite = "----calma" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream cuerpo = new ByteArrayOutputStream();
        campo(cuerpo, limite, "api_key", apiKey);
        for (Map.Entry<String, String> e : firmados.entrySet()) {
            campo(cuerpo, limite, e.getKey(), e.getValue());
        }
        campo(cuerpo, limite, "signature", firmar(firmados));
        archivo(cuerpo, limite, nombreDe(clave), tipoMime, contenido);
        escribir(cuerpo, "--" + limite + "--\r\n");

        HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlBase + "/" + cloudName + "/raw/upload"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "multipart/form-data; boundary=" + limite)
                .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo.toByteArray()))
                .build();

        HttpResponse<String> respuesta = enviar(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (respuesta.statusCode() != 200) {
            throw new AlmacenamientoException("Cloudinary rechazó la subida (HTTP " + respuesta.statusCode() + ")");
        }
        String idDevuelto = extraer(PUBLIC_ID, respuesta.body());
        String url = extraer(SECURE_URL, respuesta.body());
        if (idDevuelto == null || url == null) {
            throw new AlmacenamientoException("Respuesta inesperada de Cloudinary al subir el archivo");
        }
        return new ArchivoAlmacenado(idDevuelto, url);
    }

    @Override
    public byte[] descargar(String publicId) {
        Map<String, String> firmados = parametrosFirmados(publicId);
        String consulta = "api_key=" + codificar(apiKey)
                + "&public_id=" + codificar(publicId)
                + "&signature=" + firmar(firmados)
                + "&timestamp=" + firmados.get("timestamp")
                + "&type=" + TIPO_ENTREGA;

        HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlBase + "/" + cloudName + "/raw/download?" + consulta))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        HttpResponse<byte[]> respuesta = enviar(peticion, HttpResponse.BodyHandlers.ofByteArray());
        if (respuesta.statusCode() != 200) {
            throw new AlmacenamientoException("Cloudinary no entregó el archivo (HTTP " + respuesta.statusCode() + ")");
        }
        return respuesta.body();
    }

    @Override
    public void eliminar(String publicId) {
        Map<String, String> firmados = parametrosFirmados(publicId);
        String formulario = "api_key=" + codificar(apiKey)
                + "&public_id=" + codificar(publicId)
                + "&signature=" + firmar(firmados)
                + "&timestamp=" + firmados.get("timestamp")
                + "&type=" + TIPO_ENTREGA;

        HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlBase + "/" + cloudName + "/raw/destroy"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formulario))
                .build();

        HttpResponse<String> respuesta = enviar(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String resultado = extraer(RESULTADO, respuesta.body());
        if (respuesta.statusCode() != 200 || resultado == null || !(resultado.equals("ok") || resultado.equals("not found"))) {
            throw new AlmacenamientoException("Cloudinary no eliminó el archivo (HTTP " + respuesta.statusCode() + ")");
        }
    }

    // ----- auxiliares -----

    private Map<String, String> parametrosFirmados(String publicId) {
        Map<String, String> p = new TreeMap<>();
        p.put("public_id", publicId);
        p.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));
        p.put("type", TIPO_ENTREGA);
        return p;
    }

    /** SHA-1 (hex) de "k1=v1&k2=v2" + api_secret. Los parámetros llegan ya ordenados (TreeMap). */
    private String firmar(Map<String, String> parametros) {
        StringBuilder aFirmar = new StringBuilder();
        parametros.forEach((k, v) -> {
            if (aFirmar.length() > 0) {
                aFirmar.append('&');
            }
            aFirmar.append(k).append('=').append(v);
        });
        aFirmar.append(apiSecret);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(aFirmar.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 no disponible", e);
        }
    }

    private <T> HttpResponse<T> enviar(HttpRequest peticion, HttpResponse.BodyHandler<T> manejador) {
        try {
            return http.send(peticion, manejador);
        } catch (IOException e) {
            throw new AlmacenamientoException("No se pudo conectar con Cloudinary", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AlmacenamientoException("La conexión con Cloudinary fue interrumpida", e);
        }
    }

    private static String extraer(Pattern patron, String json) {
        Matcher m = patron.matcher(json == null ? "" : json);
        return m.find() ? m.group(1).replace("\\/", "/") : null;
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private static String nombreDe(String clave) {
        return clave.substring(clave.lastIndexOf('/') + 1);
    }

    private static void campo(ByteArrayOutputStream salida, String limite, String nombre, String valor) {
        escribir(salida, "--" + limite + "\r\nContent-Disposition: form-data; name=\"" + nombre + "\"\r\n\r\n" + valor + "\r\n");
    }

    private static void archivo(ByteArrayOutputStream salida, String limite, String nombreArchivo, String tipoMime, byte[] contenido) {
        escribir(salida, "--" + limite + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + nombreArchivo
                + "\"\r\nContent-Type: " + tipoMime + "\r\n\r\n");
        salida.writeBytes(contenido);
        escribir(salida, "\r\n");
    }

    private static void escribir(ByteArrayOutputStream salida, String texto) {
        salida.writeBytes(texto.getBytes(StandardCharsets.UTF_8));
    }
}
