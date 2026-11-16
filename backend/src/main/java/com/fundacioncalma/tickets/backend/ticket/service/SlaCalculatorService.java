package com.fundacioncalma.tickets.backend.ticket.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Service;

/**
 * Cálculo de SLA en horario laboral de la Fundación CALMA: lunes a viernes, de 8:00 a 17:00.
 * El contador no corre de noche ni en fin de semana (Acta de Refinamiento, sección 3).
 * Todo se calcula en minutos para no perder fracciones de hora.
 */
@Service
public class SlaCalculatorService {

    private static final LocalTime HORA_INICIO_LABORAL = LocalTime.of(8, 0);
    private static final LocalTime HORA_FIN_LABORAL = LocalTime.of(17, 0);

    /**
     * Fecha de vencimiento de un SLA expresado en horas laborales.
     *
     * @param fechaInicio Cuándo se creó o reanudó el ticket.
     * @param slaHoras    Horas laborales para resolver (ej. 4, 24, 48, 72).
     */
    public LocalDateTime calcularFechaVencimientoSla(LocalDateTime fechaInicio, int slaHoras) {
        return sumarMinutosLaborales(fechaInicio, slaHoras * 60L);
    }

    /** Suma minutos laborales a una fecha, saltando noches, fines de semana y horas fuera de horario. */
    public LocalDateTime sumarMinutosLaborales(LocalDateTime inicio, long minutos) {
        LocalDateTime actual = ajustarAHorarioLaboral(inicio);
        long restantes = minutos;

        while (restantes > 0) {
            LocalDateTime finDelDia = actual.toLocalDate().atTime(HORA_FIN_LABORAL);
            long disponiblesHoy = Duration.between(actual, finDelDia).toMinutes();

            if (restantes <= disponiblesHoy) {
                return actual.plusMinutes(restantes);
            }
            restantes -= disponiblesHoy;
            actual = ajustarAHorarioLaboral(finDelDia); // pasa al siguiente día laboral a las 8:00
        }
        return actual;
    }

    /** Minutos laborales (L-V 8:00 a 17:00) transcurridos entre dos instantes. */
    public long minutosLaboralesEntre(LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null || hasta == null || !hasta.isAfter(desde)) {
            return 0;
        }
        long total = 0;
        for (LocalDate dia = desde.toLocalDate(); !dia.isAfter(hasta.toLocalDate()); dia = dia.plusDays(1)) {
            if (esFinDeSemana(dia)) {
                continue;
            }
            LocalDateTime inicioVentana = maximo(desde, dia.atTime(HORA_INICIO_LABORAL));
            LocalDateTime finVentana = minimo(hasta, dia.atTime(HORA_FIN_LABORAL));
            if (finVentana.isAfter(inicioVentana)) {
                total += Duration.between(inicioVentana, finVentana).toMinutes();
            }
        }
        return total;
    }

    /** Si la fecha cae fuera del horario laboral, la lleva al inicio del siguiente tramo laboral. */
    private LocalDateTime ajustarAHorarioLaboral(LocalDateTime fecha) {
        LocalDateTime actual = fecha;
        while (true) {
            LocalDate dia = actual.toLocalDate();
            if (esFinDeSemana(dia)) {
                actual = dia.plusDays(1).atTime(HORA_INICIO_LABORAL);
            } else if (actual.toLocalTime().isBefore(HORA_INICIO_LABORAL)) {
                return dia.atTime(HORA_INICIO_LABORAL);
            } else if (!actual.toLocalTime().isBefore(HORA_FIN_LABORAL)) {
                actual = dia.plusDays(1).atTime(HORA_INICIO_LABORAL);
            } else {
                return actual;
            }
        }
    }

    private boolean esFinDeSemana(LocalDate dia) {
        return dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private LocalDateTime maximo(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalDateTime minimo(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }
}
