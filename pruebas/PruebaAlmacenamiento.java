package alm;

import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

public class Prueba {
    static int fallos = 0;
    static void ok(String nombre, boolean cond) { if (!cond) fallos++; System.out.println((cond ? "OK   " : "FALLA") + " " + nombre); }
    static byte[] b(int... v) { byte[] r = new byte[v.length + 20]; for (int i = 0; i < v.length; i++) r[i] = (byte) v[i]; return r; }
    static ArchivoInvalidoException.Motivo rechazo(String nombre, byte[] datos) {
        try { ValidadorArchivo.validar(nombre, datos, 5 * 1024 * 1024); return null; } catch (ArchivoInvalidoException e) { return e.getMotivo(); }
    }
    static String sha1(String s) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8))); }

    public static void main(String[] args) throws Exception {
        System.out.println("== Validador de archivos");
        ok("PDF válido", rechazo("informe.pdf", "%PDF-1.7 contenido".getBytes()) == null);
        ok("PNG válido", rechazo("captura.PNG", b(0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A)) == null);
        ok("JPEG válido (.jpg y .jpeg)", rechazo("foto.jpg", b(0xFF,0xD8,0xFF,0xE0)) == null && rechazo("foto.jpeg", b(0xFF,0xD8,0xFF,0xE0)) == null);
        ok("DOCX válido (cabecera PK)", rechazo("acta.docx", b(0x50,0x4B,0x03,0x04)) == null);
        ok("XLS antiguo válido (cabecera OLE)", rechazo("datos.xls", b(0xD0,0xCF,0x11,0xE0,0xA1,0xB1,0x1A,0xE1)) == null);
        ok("TXT válido", rechazo("notas.txt", "hola mundo".getBytes()) == null);
        ok("WEBP válido", rechazo("a.webp", "RIFF1234WEBPVP8 ".getBytes()) == null);
        ok("Ejecutable (.exe) rechazado por tipo", rechazo("virus.exe", b('M','Z')) == ArchivoInvalidoException.Motivo.TIPO_NO_PERMITIDO);
        ok("Doble extensión informe.pdf.exe rechazada", rechazo("informe.pdf.exe", "%PDF-1.7".getBytes()) == ArchivoInvalidoException.Motivo.TIPO_NO_PERMITIDO);
        ok("SVG y HTML rechazados", rechazo("x.svg", "<svg/>".getBytes()) == ArchivoInvalidoException.Motivo.TIPO_NO_PERMITIDO && rechazo("x.html", "<html>".getBytes()) == ArchivoInvalidoException.Motivo.TIPO_NO_PERMITIDO);
        ok("ZIP rechazado", rechazo("x.zip", b(0x50,0x4B,0x03,0x04)) == ArchivoInvalidoException.Motivo.TIPO_NO_PERMITIDO);
        ok("Ejecutable renombrado como .pdf rechazado por contenido", rechazo("falso.pdf", b('M','Z',0x90,0)) == ArchivoInvalidoException.Motivo.CONTENIDO_NO_COINCIDE);
        ok("PDF con extensión .png rechazado por contenido", rechazo("falso.png", "%PDF-1.7".getBytes()) == ArchivoInvalidoException.Motivo.CONTENIDO_NO_COINCIDE);
        ok("TXT con bytes nulos (binario) rechazado", rechazo("raro.txt", b('a',0,'b')) == ArchivoInvalidoException.Motivo.CONTENIDO_NO_COINCIDE);
        ok("Archivo vacío rechazado", rechazo("a.pdf", new byte[0]) == ArchivoInvalidoException.Motivo.VACIO);
        ok("Más de 5 MB rechazado", rechazo("grande.pdf", new byte[5 * 1024 * 1024 + 1]) == ArchivoInvalidoException.Motivo.MUY_GRANDE);
        ok("Sin extensión rechazado", rechazo("sinextension", "x".getBytes()) == ArchivoInvalidoException.Motivo.NOMBRE_INVALIDO);
        var r = ValidadorArchivo.validar("C:\\Users\\ana\\Mi informe (final) ñ.pdf", "%PDF-1.4 x".getBytes(), 1000);
        ok("Ruta de Windows removida: '" + r.nombreOriginal() + "'", r.nombreOriginal().equals("Mi informe (final) ñ.pdf"));
        ok("Nombre seguro para la ruta: '" + r.nombreSeguro() + "'", r.nombreSeguro().equals("Mi_informe_final_.pdf") || r.nombreSeguro().matches("[A-Za-z0-9._-]+"));
        var largo = ValidadorArchivo.validar("a".repeat(300) + ".pdf", "%PDF-1.4 x".getBytes(), 1000);
        ok("Nombre largo se acorta conservando la extensión (" + largo.nombreOriginal().length() + " caracteres)", largo.nombreOriginal().length() == 150 && largo.nombreOriginal().endsWith(".pdf"));
        ok("MIME de DOCX completo y menor a 100 caracteres", ValidadorArchivo.validar("a.docx", b(0x50,0x4B,0x03,0x04), 1000).tipoMime().length() < 100);

        System.out.println("\n== Almacenamiento local");
        Path tmp = Files.createTempDirectory("adj");
        var local = new LocalAlmacenamiento(tmp.toString());
        var g = local.guardar("TKT-1/uuid_informe.pdf", "application/pdf", "contenido".getBytes());
        ok("guardar y descargar devuelven el mismo contenido", new String(local.descargar(g.publicId())).equals("contenido"));
        local.eliminar(g.publicId());
        ok("eliminar borra el archivo", !Files.exists(tmp.resolve("TKT-1/uuid_informe.pdf")));
        local.eliminar(g.publicId()); ok("eliminar dos veces no falla", true);
        try { local.guardar("../escapa.txt", "text/plain", "x".getBytes()); ok("ruta con ../ bloqueada", false); } catch (AlmacenamientoException e) { ok("ruta con ../ bloqueada", true); }

        System.out.println("\n== Cloudinary contra un servidor simulado (verifica la firma recalculándola de forma independiente)");
        final String SECRET = "secreto-de-prueba", KEY = "123456", CLOUD = "demo";
        List<String> log = new ArrayList<>();
        HttpServer srv = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final byte[] guardado = "ARCHIVO-EN-CLOUDINARY".getBytes();
        srv.createContext("/v1_1/demo/raw/upload", ex -> {
            byte[] cuerpo = ex.getRequestBody().readAllBytes(); String txt = new String(cuerpo, StandardCharsets.ISO_8859_1);
            String ct = ex.getRequestHeaders().getFirst("Content-Type"); String limite = ct.substring(ct.indexOf("boundary=") + 9);
            Map<String,String> campos = new HashMap<>(); String archivo = null;
            for (String parte : txt.split("--" + limite)) {
                var m = java.util.regex.Pattern.compile("name=\"([^\"]+)\"(; filename=\"([^\"]+)\")?.*?\r\n\r\n(.*)\r\n$", java.util.regex.Pattern.DOTALL).matcher(parte.trim().isEmpty() ? "" : parte.substring(2));
                if (m.find()) { if (m.group(3) != null) archivo = m.group(4); else campos.put(m.group(1), m.group(4)); }
            }
            try {
                String esperada = sha1("public_id=" + campos.get("public_id") + "&timestamp=" + campos.get("timestamp") + "&type=" + campos.get("type") + SECRET);
                log.add("upload firma_ok=" + esperada.equals(campos.get("signature")) + " api_key_ok=" + KEY.equals(campos.get("api_key")) + " type=" + campos.get("type") + " public_id=" + campos.get("public_id") + " archivo=" + archivo);
            } catch (Exception e) { log.add("error " + e); }
            byte[] resp = ("{\"public_id\":\"" + campos.get("public_id") + "\",\"secure_url\":\"https:\\/\\/res.cloudinary.com\\/demo\\/raw\\/authenticated\\/v1\\/" + campos.get("public_id") + "\",\"bytes\":21}").getBytes();
            ex.sendResponseHeaders(200, resp.length); ex.getResponseBody().write(resp); ex.close();
        });
        srv.createContext("/v1_1/demo/raw/download", ex -> {
            Map<String,String> q = new HashMap<>(); for (String kv : ex.getRequestURI().getRawQuery().split("&")) { String[] p = kv.split("=", 2); q.put(p[0], java.net.URLDecoder.decode(p[1], StandardCharsets.UTF_8)); }
            try { String esperada = sha1("public_id=" + q.get("public_id") + "&timestamp=" + q.get("timestamp") + "&type=" + q.get("type") + SECRET);
                  boolean firmaOk = esperada.equals(q.get("signature")); log.add("download firma_ok=" + firmaOk + " api_key_ok=" + KEY.equals(q.get("api_key")) + " public_id=" + q.get("public_id"));
                  if (!firmaOk) { ex.sendResponseHeaders(401, -1); ex.close(); return; } } catch (Exception e) { log.add("error " + e); }
            ex.sendResponseHeaders(200, guardado.length); ex.getResponseBody().write(guardado); ex.close();
        });
        final String[] resultadoDestroy = {"ok"};
        srv.createContext("/v1_1/demo/raw/destroy", ex -> {
            String form = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); Map<String,String> q = new HashMap<>();
            for (String kv : form.split("&")) { String[] p = kv.split("=", 2); q.put(p[0], java.net.URLDecoder.decode(p[1], StandardCharsets.UTF_8)); }
            try { log.add("destroy firma_ok=" + sha1("public_id=" + q.get("public_id") + "&timestamp=" + q.get("timestamp") + "&type=" + q.get("type") + SECRET).equals(q.get("signature"))); } catch (Exception e) {}
            byte[] resp = ("{\"result\":\"" + resultadoDestroy[0] + "\"}").getBytes(); int estado = resultadoDestroy[0].equals("error") ? 401 : 200;
            ex.sendResponseHeaders(estado, resp.length); ex.getResponseBody().write(resp); ex.close();
        });
        srv.start();
        String base = "http://127.0.0.1:" + srv.getAddress().getPort() + "/v1_1";
        var c = new CloudinaryAlmacenamiento(CLOUD, KEY, SECRET, "calma-tickets", base);
        byte[] contenido = new byte[2000]; new Random(1).nextBytes(contenido);
        var sub = c.guardar("TKT-1/uuid_informe final.pdf", "application/pdf", "ARCHIVO-EN-CLOUDINARY".getBytes());
        ok("subida: " + log.get(0), log.get(0).contains("firma_ok=true") && log.get(0).contains("api_key_ok=true") && log.get(0).contains("type=authenticated") && log.get(0).contains("public_id=calma-tickets/TKT-1/uuid_informe final.pdf") && log.get(0).contains("archivo=ARCHIVO-EN-CLOUDINARY"));
        ok("subida devuelve public_id y url sin las barras escapadas: " + sub.url(), sub.publicId().equals("calma-tickets/TKT-1/uuid_informe final.pdf") && sub.url().startsWith("https://res.cloudinary.com/demo/raw/authenticated/"));
        byte[] bajado = c.descargar(sub.publicId());
        ok("descarga devuelve el contenido y la firma es válida: " + log.get(1), new String(bajado).equals("ARCHIVO-EN-CLOUDINARY") && log.get(1).contains("firma_ok=true") && log.get(1).contains("public_id=calma-tickets/TKT-1/uuid_informe final.pdf"));
        c.eliminar(sub.publicId());
        ok("eliminar con firma válida: " + log.get(2), log.get(2).contains("firma_ok=true"));
        resultadoDestroy[0] = "not found"; c.eliminar("x"); ok("eliminar de un archivo inexistente no falla", true);
        resultadoDestroy[0] = "error"; try { c.eliminar("x"); ok("error de Cloudinary se informa", false); } catch (AlmacenamientoException e) { ok("error de Cloudinary se informa", true); }
        srv.stop(0);
        try { c.descargar("x"); ok("sin conexión se informa", false); } catch (AlmacenamientoException e) { ok("servidor caído se informa como AlmacenamientoException", true); }
        try { new CloudinaryAlmacenamiento("", "", "", "c", base); ok("sin credenciales falla al arrancar", false); } catch (IllegalStateException e) { ok("sin credenciales falla al arrancar", true); }
        System.out.println(fallos == 0 ? "\nTodas las pruebas pasaron" : "\nFALLOS: " + fallos);
        System.exit(fallos == 0 ? 0 : 1);
    }
}
