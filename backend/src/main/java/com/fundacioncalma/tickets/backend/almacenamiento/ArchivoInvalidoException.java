package com.fundacioncalma.tickets.backend.almacenamiento;

/** El archivo no cumple las políticas de adjuntos (tamaño, tipo o contenido). */
public class ArchivoInvalidoException extends Exception {

    public enum Motivo { VACIO, MUY_GRANDE, NOMBRE_INVALIDO, TIPO_NO_PERMITIDO, CONTENIDO_NO_COINCIDE }

    private final Motivo motivo;

    public ArchivoInvalidoException(Motivo motivo, String mensaje) {
        super(mensaje);
        this.motivo = motivo;
    }

    public Motivo getMotivo() {
        return motivo;
    }
}
