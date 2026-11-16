package com.fundacioncalma.tickets.backend.rrhh.dto;

import java.util.UUID;

/** Datos mínimos de un empleado de RR. HH. para elegirlo como representante de un área. */
public record EmpleadoDTO(UUID id, String nombre, String correo, String cargo) {}
