package com.fundacioncalma.tickets.backend.almacenamiento;

/** Fallo del proveedor de almacenamiento (red, credenciales, disco). */
public class AlmacenamientoException extends RuntimeException {
    public AlmacenamientoException(String mensaje) {
        super(mensaje);
    }

    public AlmacenamientoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
