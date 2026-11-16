package com.fundacioncalma.tickets.backend.rrhh.dto;

import java.time.Instant;

public record EstadoSincronizacionRrhhDTO(
        boolean habilitada,
        String estado,
        Instant ultimaConexion,
        Instant ultimaSincronizacion,
        Instant ultimoIntento,
        String ultimoError,
        long cursor,
        long registrosAplicados,
        long intervaloMinutos,
        Instant ultimoIntentoManual,
        long cooldownManualSegundos) {}
