package com.fundacioncalma.tickets.backend.ticket.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Adjunto tal como lo ve el frontend. No incluye la URL del proveedor: los archivos solo se descargan
 * a través del backend, que verifica los permisos del usuario.
 */
public record AdjuntoDTO(
        UUID id,
        String nombre,
        String tipo,
        int tamano,
        String subidoPor,
        LocalDateTime fecha,
        boolean puedeEliminar,
        boolean purgado) {}
