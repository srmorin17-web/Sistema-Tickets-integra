package com.fundacioncalma.tickets.backend.almacenamiento;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import com.fundacioncalma.tickets.backend.almacenamiento.ArchivoInvalidoException.Motivo;

/**
 * Política de adjuntos: lista blanca de tipos y comprobación de que el contenido coincide con la extensión
 * (los primeros bytes del archivo), para impedir, por ejemplo, un ejecutable renombrado como .pdf.
 * No se aceptan HTML, SVG, scripts ni archivos comprimidos.
 */
public final class ValidadorArchivo {

    private record Tipo(String mime, Predicate<byte[]> firma) {}

    private static final String OOXML = "application/vnd.openxmlformats-officedocument.";

    private static final Predicate<byte[]> ZIP = b -> empieza(b, 0, 0x50, 0x4B, 0x03, 0x04);
    private static final Predicate<byte[]> OLE = b -> empieza(b, 0, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
    private static final Predicate<byte[]> TEXTO = b -> {
        int limite = Math.min(b.length, 8192);
        for (int i = 0; i < limite; i++) {
            if (b[i] == 0) {
                return false; // un byte nulo indica contenido binario
            }
        }
        return true;
    };

    private static final Map<String, Tipo> PERMITIDOS = Map.ofEntries(
            Map.entry("pdf", new Tipo("application/pdf", ValidadorArchivo::esPdf)),
            Map.entry("png", new Tipo("image/png", b -> empieza(b, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))),
            Map.entry("jpg", new Tipo("image/jpeg", b -> empieza(b, 0, 0xFF, 0xD8, 0xFF))),
            Map.entry("jpeg", new Tipo("image/jpeg", b -> empieza(b, 0, 0xFF, 0xD8, 0xFF))),
            Map.entry("gif", new Tipo("image/gif", b -> empieza(b, 0, 'G', 'I', 'F', '8'))),
            Map.entry("webp", new Tipo("image/webp", b -> empieza(b, 0, 'R', 'I', 'F', 'F') && empieza(b, 8, 'W', 'E', 'B', 'P'))),
            Map.entry("txt", new Tipo("text/plain", TEXTO)),
            Map.entry("log", new Tipo("text/plain", TEXTO)),
            Map.entry("csv", new Tipo("text/csv", TEXTO)),
            Map.entry("docx", new Tipo(OOXML + "wordprocessingml.document", ZIP)),
            Map.entry("xlsx", new Tipo(OOXML + "spreadsheetml.sheet", ZIP)),
            Map.entry("pptx", new Tipo(OOXML + "presentationml.presentation", ZIP)),
            Map.entry("doc", new Tipo("application/msword", OLE)),
            Map.entry("xls", new Tipo("application/vnd.ms-excel", OLE)),
            Map.entry("ppt", new Tipo("application/vnd.ms-powerpoint", OLE)));

    private ValidadorArchivo() {}

    /** Resultado de una validación correcta. nombreSeguro sirve para armar la clave en el almacenamiento. */
    public record Resultado(String nombreOriginal, String nombreSeguro, String tipoMime) {}

    public static Resultado validar(String nombreRecibido, byte[] contenido, long maxBytes) throws ArchivoInvalidoException {
        if (contenido == null || contenido.length == 0) {
            throw new ArchivoInvalidoException(Motivo.VACIO, "El archivo está vacío");
        }
        if (contenido.length > maxBytes) {
            throw new ArchivoInvalidoException(Motivo.MUY_GRANDE, "El archivo supera el tamaño máximo permitido");
        }

        String nombre = limpiarNombre(nombreRecibido);
        int punto = nombre.lastIndexOf('.');
        if (nombre.isEmpty() || punto <= 0 || punto == nombre.length() - 1) {
            throw new ArchivoInvalidoException(Motivo.NOMBRE_INVALIDO, "El archivo debe tener un nombre y una extensión");
        }

        String extension = nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
        Tipo tipo = PERMITIDOS.get(extension);
        if (tipo == null) {
            throw new ArchivoInvalidoException(Motivo.TIPO_NO_PERMITIDO, "Tipo de archivo no permitido: ." + extension);
        }
        if (!tipo.firma().test(contenido)) {
            throw new ArchivoInvalidoException(Motivo.CONTENIDO_NO_COINCIDE, "El contenido no corresponde a un archivo ." + extension);
        }
        return new Resultado(nombre, nombreSeguro(nombre), tipo.mime());
    }

    /** Extensiones aceptadas, en orden alfabético (para mensajes y para el frontend). */
    public static java.util.List<String> extensionesPermitidas() {
        return PERMITIDOS.keySet().stream().sorted().toList();
    }

    /** Quita la ruta que algunos navegadores envían, los caracteres de control y limita la longitud conservando la extensión. */
    static String limpiarNombre(String recibido) {
        if (recibido == null) {
            return "";
        }
        String nombre = recibido.substring(Math.max(recibido.lastIndexOf('/'), recibido.lastIndexOf('\\')) + 1);
        nombre = nombre.replaceAll("\\p{Cntrl}", "").trim();
        if (nombre.length() > 150) {
            int punto = nombre.lastIndexOf('.');
            String extension = punto > 0 ? nombre.substring(punto) : "";
            nombre = nombre.substring(0, 150 - extension.length()) + extension;
        }
        return nombre;
    }

    /** Solo letras, números, punto, guion y guion bajo: es seguro como parte de una ruta o URL. */
    static String nombreSeguro(String nombre) {
        return nombre.replaceAll("[^A-Za-z0-9._-]", "_").replaceAll("_{2,}", "_");
    }

    private static boolean esPdf(byte[] b) {
        // La especificación admite la cabecera dentro de los primeros 1024 bytes
        int limite = Math.min(b.length, 1024) - 4;
        byte[] marca = "%PDF-".getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i <= limite; i++) {
            if (empieza(b, i, marca[0], marca[1], marca[2], marca[3], marca[4])) {
                return true;
            }
        }
        return false;
    }

    private static boolean empieza(byte[] datos, int desplazamiento, int... esperado) {
        if (datos.length < desplazamiento + esperado.length) {
            return false;
        }
        for (int i = 0; i < esperado.length; i++) {
            if ((datos[desplazamiento + i] & 0xFF) != (esperado[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }
}
