package com.fundacioncalma.tickets.backend.rrhh.cliente;

/** RR. HH. informa que el empleado no está activo (estadoLaboral distinto del valor configurado en rrhh.estado-activo). */
public class EmpleadoInactivoException extends RuntimeException {
    public EmpleadoInactivoException(String empleado) {
        super("El empleado no está activo en RR. HH.: " + empleado);
    }
}
