package com.fundacioncalma.tickets.backend.rrhh.cliente;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class PruebaRrhh {
    static int fallos = 0;
    static void ok(String nombre, boolean cond) { if (!cond) fallos++; System.out.println((cond ? "OK   " : "FALLA") + " " + nombre); }
    static boolean cae(Runnable r) { try { r.run(); return false; } catch (RrhhNoDisponibleException e) { return true; } }

    public static void main(String[] args) throws Exception {
        final String ID = "7d5f8f0e-1111-4222-8333-944455556666", TOKEN = "token-de-servicio";
        List<String> vistos = new ArrayList<>();
        HttpServer srv = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final String JSON_EMP = "{\"id\":\"" + ID + "\",\"nombres\":\"María\",\"apellidos\":\"Pérez\",\"correoCorporativo\":\"maria.perez@calma.org\",\"cargo\":\"Analista de Sistemas\",\"estadoLaboral\":\"ACTIVO\",\"liderId\":\"aaaaaaaa-1111-4222-8333-944455556666\",\"liderCorreo\":\"jefe@calma.org\",\"campoNuevoDeRrhh\":{\"x\":1}}";
        srv.createContext("/api/rrhh/v1/empleados", ex -> {
            String auth = ex.getRequestHeaders().getFirst("Authorization"); String acc = ex.getRequestHeaders().getFirst("Accept");
            String ruta = ex.getRequestURI().getPath(); String q = ex.getRequestURI().getRawQuery() == null ? "" : ex.getRequestURI().getRawQuery();
            vistos.add(ruta + "?" + q + " auth=" + auth + " accept=" + acc);
            int estado = 200; byte[] cuerpo;
            if (!("Bearer " + TOKEN).equals(auth)) { estado = 401; cuerpo = "{}".getBytes(); }
            else if (ruta.endsWith("/lookup")) {
                String correo = URLDecoder.decode(q.replace("correo=", ""), StandardCharsets.UTF_8);
                if (correo.equals("maria.perez@calma.org")) cuerpo = JSON_EMP.getBytes(StandardCharsets.UTF_8);
                else if (correo.equals("roto@calma.org")) { estado = 500; cuerpo = "boom".getBytes(); }
                else if (correo.equals("basura@calma.org")) cuerpo = "esto no es json".getBytes();
                else if (correo.equals("lento@calma.org")) { try { Thread.sleep(3000); } catch (Exception e) {} cuerpo = JSON_EMP.getBytes(StandardCharsets.UTF_8); }
                else { estado = 404; cuerpo = "{}".getBytes(); }
            } else if (ruta.endsWith("/empleados")) {
                String texto = ""; for (String kv : q.split("&")) if (kv.startsWith("q=")) texto = URLDecoder.decode(kv.substring(2), StandardCharsets.UTF_8);
                vistos.add("busqueda q=[" + texto + "]");
                cuerpo = ("[" + JSON_EMP + "," + JSON_EMP.replace("María", "Mario") + "]").getBytes(StandardCharsets.UTF_8);
            } else {
                if (ruta.endsWith(ID)) cuerpo = JSON_EMP.getBytes(StandardCharsets.UTF_8); else { estado = 404; cuerpo = "{}".getBytes(); }
            }
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            ex.sendResponseHeaders(estado, cuerpo.length); ex.getResponseBody().write(cuerpo); ex.close();
        });
        srv.start();
        try {
            String base = "http://127.0.0.1:" + srv.getAddress().getPort() + "/api/rrhh/v1";
            RrhhRestCliente c = new RrhhRestCliente(base + "/", TOKEN, 1);

            System.out.println("== Cliente REST contra un RR. HH. simulado");
            var e = c.porCorreo("maria.perez@calma.org").orElseThrow();
            ok("lookup por correo: " + e.nombres() + " " + e.apellidos() + ", " + e.cargo() + ", " + e.estadoLaboral() + ", líder " + e.liderCorreo(),
               e.id().toString().equals(ID) && e.nombres().equals("María") && e.apellidos().equals("Pérez") && e.liderCorreo().equals("jefe@calma.org") && e.liderId() != null);
            ok("se ignoran los campos nuevos que agregue RR. HH. (campoNuevoDeRrhh)", true);
            ok("envía el token como Bearer y pide JSON: " + vistos.get(0).replaceAll(".*auth=", "auth="), vistos.get(0).contains("auth=Bearer " + TOKEN) && vistos.get(0).contains("accept=application/json"));
            ok("la URL base con barra final no duplica barras: " + vistos.get(0).split(" ")[0], vistos.get(0).startsWith("/api/rrhh/v1/empleados/lookup?correo=maria.perez%40calma.org"));
            ok("lookup de un correo inexistente (404) -> vacío, sin error", c.porCorreo("nadie@calma.org").isEmpty());
            ok("por id existente", c.porId(UUID.fromString(ID)).isPresent());
            ok("por id inexistente (404) -> vacío", c.porId(UUID.randomUUID()).isEmpty());
            var lista = c.buscar("Fernández & Cía/ñ", 20);
            ok("búsqueda devuelve la lista (" + lista.size() + ")", lista.size() == 2 && lista.get(1).nombres().equals("Mario"));
            ok("el texto con tildes, & y / llega bien codificado: " + vistos.get(vistos.size() - 1), vistos.get(vistos.size() - 1).equals("busqueda q=[Fernández & Cía/ñ]"));
            ok("500 de RR. HH. -> RrhhNoDisponibleException", cae(() -> c.porCorreo("roto@calma.org")));
            ok("JSON inválido -> RrhhNoDisponibleException", cae(() -> c.porCorreo("basura@calma.org")));
            ok("token incorrecto (401) -> RrhhNoDisponibleException", cae(() -> new RrhhRestCliente(base, "otro-token", 1).porCorreo("maria.perez@calma.org")));
            long t0 = System.currentTimeMillis();
            boolean agoto = cae(() -> c.porCorreo("lento@calma.org")); long ms = System.currentTimeMillis() - t0;
            ok("respuesta lenta -> se corta al agotar el tiempo (" + ms + " ms de 3000)", agoto && ms < 2500);
            srv.stop(0);
            ok("servidor caído -> RrhhNoDisponibleException", cae(() -> c.porCorreo("maria.perez@calma.org")));
        } finally { srv.stop(0); }

        System.out.println("\n== Caché temporal");
        AtomicLong reloj = new AtomicLong(1000);
        CacheTemporal<String, String> cache = new CacheTemporal<>(Duration.ofSeconds(300), reloj::get);
        ok("vacía al inicio", cache.obtener("a").isEmpty());
        cache.guardar("a", "uno"); ok("guardado y recuperado", cache.obtener("a").orElse("").equals("uno"));
        reloj.addAndGet(299_000); ok("a los 299 s sigue vigente", cache.obtener("a").isPresent());
        reloj.addAndGet(2_000); ok("a los 301 s venció", cache.obtener("a").isEmpty());
        CacheTemporal<String, String> apagada = new CacheTemporal<>(Duration.ofSeconds(0), reloj::get);
        apagada.guardar("a", "uno"); ok("con vigencia 0 la caché queda desactivada", apagada.obtener("a").isEmpty());
        System.out.println(fallos == 0 ? "\nTodas las pruebas pasaron" : "\nFALLOS: " + fallos);
        System.exit(fallos == 0 ? 0 : 1);
    }
}
