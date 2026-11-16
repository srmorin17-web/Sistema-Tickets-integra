package com.fundacioncalma.tickets.backend.ticket.dto;

import java.util.List;
import java.util.UUID;

/**
 * Catálogos de solo lectura que el frontend necesita para armar formularios
 * y acciones (los UUID se generan en la base de datos y no pueden conocerse de antemano).
 * Solo incluye elementos activos.
 */
public record CatalogosResponse(
        List<CategoriaItem> categorias,
        List<PrioridadItem> prioridades,
        List<EstadoItem> estados,
        List<AreaItem> areasEscalamiento) {

    /** prioridad y slaHoras: los que el sistema asignará a los tickets de la categoría. */
    public record CategoriaItem(UUID id, String nombre, String descripcion, String prioridad, Integer slaHoras) {}

    public record PrioridadItem(UUID id, String nombre, Integer slaHoras) {}

    public record EstadoItem(UUID id, String nombre, String colorHex, Integer orden, boolean esEstadoFinal) {}

    /** representante: nombre del empleado que recibe los tickets escalados; nulo si el área no tiene uno. */
    public record AreaItem(UUID id, String nombre, String descripcion, String representante) {}
}
