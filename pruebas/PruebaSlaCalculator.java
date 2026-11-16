package p;
import java.time.LocalDateTime;
public class Prueba {
    static int fallos = 0;
    static void ok(String nombre, Object real, Object esperado) {
        boolean bien = real.equals(esperado);
        if (!bien) fallos++;
        System.out.println((bien ? "OK   " : "FALLA") + " " + nombre + " -> " + real + (bien ? "" : "  (esperado " + esperado + ")"));
    }
    public static void main(String[] a) {
        SlaCalculatorService s = new SlaCalculatorService();
        // 2026-11-02 es lunes; 2026-11-06 viernes
        ok("Viernes 16:00 + 4 h (acta: reanuda el lunes 8:00)", s.calcularFechaVencimientoSla(LocalDateTime.of(2026,11,6,16,0), 4), LocalDateTime.of(2026,11,9,11,0));
        ok("Lunes 10:00 + 4 h", s.calcularFechaVencimientoSla(LocalDateTime.of(2026,11,2,10,0), 4), LocalDateTime.of(2026,11,2,14,0));
        ok("Lunes 16:30 + 4 h (antes se perdían 30 min)", s.calcularFechaVencimientoSla(LocalDateTime.of(2026,11,2,16,30), 4), LocalDateTime.of(2026,11,3,11,30));
        ok("Sábado 11:00 + 4 h", s.calcularFechaVencimientoSla(LocalDateTime.of(2026,11,7,11,0), 4), LocalDateTime.of(2026,11,9,12,0));
        ok("Martes 06:00 + 2 h (antes del horario)", s.calcularFechaVencimientoSla(LocalDateTime.of(2026,11,3,6,0), 2), LocalDateTime.of(2026,11,3,10,0));
        ok("Lunes 08:00 + 24 h (días de 9 h: mié 14:00)", s.calcularFechaVencimientoSla(LocalDateTime.of(2026,11,2,8,0), 24), LocalDateTime.of(2026,11,4,14,0));
        ok("Jueves 15:00 + 24 h (2+9+9+4 h: mar 12:00)", s.calcularFechaVencimientoSla(LocalDateTime.of(2026,11,5,15,0), 24), LocalDateTime.of(2026,11,10,12,0));
        ok("Exactamente 17:00 + 1 h", s.calcularFechaVencimientoSla(LocalDateTime.of(2026,11,2,17,0), 1), LocalDateTime.of(2026,11,3,9,0));
        ok("Pausa viernes 17:30 a lunes 8:30 = 30 min laborales", s.minutosLaboralesEntre(LocalDateTime.of(2026,11,6,17,30), LocalDateTime.of(2026,11,9,8,30)), 30L);
        ok("Pausa lunes 9:00 a martes 9:00 = 9 h laborales", s.minutosLaboralesEntre(LocalDateTime.of(2026,11,2,9,0), LocalDateTime.of(2026,11,3,9,0)), 540L);
        ok("Pausa sábado a domingo = 0", s.minutosLaboralesEntre(LocalDateTime.of(2026,11,7,9,0), LocalDateTime.of(2026,11,8,15,0)), 0L);
        ok("Pausa de 3 semanas = 15 días * 9 h", s.minutosLaboralesEntre(LocalDateTime.of(2026,11,2,8,0), LocalDateTime.of(2026,11,23,8,0)), 15L*540);
        // reanudación: vence mar 12:00, pausa vie 17:30 -> lun 8:30 (30 min) => vence mar 12:30
        LocalDateTime nuevo = s.sumarMinutosLaborales(LocalDateTime.of(2026,11,3,12,0), s.minutosLaboralesEntre(LocalDateTime.of(2026,11,6,17,30), LocalDateTime.of(2026,11,9,8,30)));
        ok("Reanudar tras pausa de fin de semana: solo +30 min (antes +63 h)", nuevo, LocalDateTime.of(2026,11,3,12,30));
        System.out.println(fallos == 0 ? "\nTodas las pruebas pasaron" : "\nFALLOS: " + fallos);
    }
}
