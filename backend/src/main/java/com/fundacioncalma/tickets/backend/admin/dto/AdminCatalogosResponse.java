package com.fundacioncalma.tickets.backend.admin.dto;

import java.util.List;
import java.util.UUID;

/**
 * Catálogos para la pantalla de administración. A diferencia de GET /catalogos,
 * incluye también los elementos inactivos.
 */
public record AdminCatalogosResponse(
        List<CategoriaAdmin> categorias,
        List<PrioridadAdmin> prioridades,
        List<AreaAdmin> areas) {

    public record CategoriaAdmin(
            UUID id, String nombre, String descripcion, boolean activo,
            UUID prioridadDefectoId, String prioridadDefecto) {}

    public record PrioridadAdmin(UUID id, String nombre, Integer slaHoras) {}

    /** representante: empleado de RR. HH. que recibe los tickets escalados al área (nulo si no tiene). */
    public record AreaAdmin(
            UUID id, String nombre, String descripcion, boolean activo, Representante representante) {}

    public record Representante(UUID id, String nombre, String correo, String cargo) {}
}
