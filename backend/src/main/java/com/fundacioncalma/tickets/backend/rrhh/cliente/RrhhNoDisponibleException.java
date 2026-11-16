package com.fundacioncalma.tickets.backend.rrhh.cliente;

/** La API de RR. HH. no respondió, rechazó las credenciales o entregó una respuesta inválida. */
public class RrhhNoDisponibleException extends RuntimeException {
    public RrhhNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public RrhhNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
