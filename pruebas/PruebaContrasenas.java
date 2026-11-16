package p;
import java.util.*;
public class Prueba {
    static int fallos = 0;
    static void ok(String n, boolean c) { if (!c) fallos++; System.out.println((c ? "OK   " : "FALLA") + " " + n); }
    static String v(String nueva, String actual, String correo) { return PoliticaContrasena.validar(nueva, actual, correo).orElse(null); }
    public static void main(String[] a) {
        System.out.println("== Política de contraseñas");
        ok("válida: 'Calma-Tickets7'", v("Calma-Tickets7", "otra", "ana@calma.org") == null);
        ok("muy corta: " + v("Ab3", null, null), v("Ab3", null, null) != null);
        ok("9 caracteres rechazada, 10 aceptada", v("Abcdefgh1", null, null) != null && v("Abcdefghi1", null, null) == null);
        ok("sin número: " + v("SoloLetrasAqui", null, null), v("SoloLetrasAqui", null, null) != null);
        ok("sin letra: " + v("1234567890", null, null), v("1234567890", null, null) != null);
        ok("igual a la actual: " + v("Calma-Tickets7", "Calma-Tickets7", null), v("Calma-Tickets7", "Calma-Tickets7", null) != null);
        ok("la contraseña semilla Admin123! está prohibida (aunque tenga 9 caracteres ya falla por largo)", v("Admin123!", null, null) != null);
        ok("común: 'Password123' (mayúsculas no la salvan): " + v("Password123", null, null), v("Password123", null, null) != null);
        ok("contiene el usuario del correo: " + v("anatorres-2026x", null, "anatorres@calma.org"), v("anatorres-2026x", null, "anatorres@calma.org") != null);
        ok("usuario muy corto (2 letras) no se aplica", v("Xx-clave-segura9", null, "ab@calma.org") == null);
        ok("más de 72 bytes rechazada (73 caracteres y 82 bytes con ñ)", v("A1" + "ñ".repeat(40), null, null) != null && v("A1" + "x".repeat(71), null, null) != null && v("A1" + "x".repeat(71), null, null).contains("larga"));
        ok("72 caracteres exactos aceptada", v("A1" + "x".repeat(70), null, null) == null);
        ok("nula o vacía rechazada", v(null, null, null) != null && v("", null, null) != null);
        System.out.println("\n== Generador de contraseñas temporales");
        Set<String> vistas = new HashSet<>(); boolean todasValidas = true, sinAmbiguos = true, largo12 = true;
        for (int i = 0; i < 20000; i++) {
            String c = GeneradorContrasena.generar(); vistas.add(c);
            if (c.length() != 12) largo12 = false;
            if (PoliticaContrasena.validar(c, null, "usuario.prueba@calma.org").isPresent()) todasValidas = false;
            if (c.matches(".*[IlOo01].*")) sinAmbiguos = false;
        }
        ok("20 000 contraseñas: todas de 12 caracteres", largo12);
        ok("todas cumplen la política", todasValidas);
        ok("ninguna tiene caracteres ambiguos (I l O o 0 1)", sinAmbiguos);
        ok("no se repiten (únicas: " + vistas.size() + ")", vistas.size() == 20000);
        String ejemplo = GeneradorContrasena.generar(); System.out.println("     ejemplo: " + ejemplo);
        ok("con la misma semilla se reproduce (prueba determinista)", GeneradorContrasena.generar(new Random(7)).equals(GeneradorContrasena.generar(new Random(7))));
        int mayus = 0, minus = 0, dig = 0; for (char ch : ejemplo.toCharArray()) { if (Character.isUpperCase(ch)) mayus++; else if (Character.isLowerCase(ch)) minus++; else dig++; }
        ok("el ejemplo tiene al menos 3 mayúsculas, 3 minúsculas y 3 dígitos (" + mayus + "/" + minus + "/" + dig + ")", mayus >= 3 && minus >= 3 && dig >= 3);
        System.out.println(fallos == 0 ? "\nTodas las pruebas pasaron" : "\nFALLOS: " + fallos);
        System.exit(fallos == 0 ? 0 : 1);
    }
}
