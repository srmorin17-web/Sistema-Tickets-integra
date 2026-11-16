package com.fundacioncalma.tickets.backend.almacenamiento;

/**
 * Almacenamiento de los archivos adjuntos. La implementación activa se elige con la propiedad
 * almacenamiento.proveedor: "local" (disco, por defecto, para desarrollo) o "cloudinary".
 * En la base de datos solo se guarda la URL y el identificador, nunca el archivo.
 */
public interface AlmacenamientoArchivos {

    /** @param clave ruta lógica única, p. ej. "TKT-20261103-001/8f2c..._informe.pdf" */
    ArchivoAlmacenado guardar(String clave, String tipoMime, byte[] contenido);

    byte[] descargar(String publicId);

    /** Si el archivo ya no existe se considera eliminado. */
    void eliminar(String publicId);

    /** Identificador en el proveedor y URL que se guarda en la base de datos. */
    record ArchivoAlmacenado(String publicId, String url) {}
}
