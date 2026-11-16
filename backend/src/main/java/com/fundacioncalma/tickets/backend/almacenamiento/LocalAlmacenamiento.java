package com.fundacioncalma.tickets.backend.almacenamiento;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Almacenamiento en disco para desarrollo y pruebas (almacenamiento.proveedor=local, valor por defecto). */
@Component
@ConditionalOnProperty(name = "almacenamiento.proveedor", havingValue = "local", matchIfMissing = true)
public class LocalAlmacenamiento implements AlmacenamientoArchivos {

    private final Path base;

    public LocalAlmacenamiento(@Value("${almacenamiento.local.ruta:./uploads}") String ruta) {
        this.base = Path.of(ruta).toAbsolutePath().normalize();
    }

    @Override
    public ArchivoAlmacenado guardar(String clave, String tipoMime, byte[] contenido) {
        Path destino = resolver(clave);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, contenido);
        } catch (IOException e) {
            throw new AlmacenamientoException("No se pudo guardar el archivo en disco", e);
        }
        return new ArchivoAlmacenado(clave, "local/" + clave);
    }

    @Override
    public byte[] descargar(String publicId) {
        try {
            return Files.readAllBytes(resolver(publicId));
        } catch (IOException e) {
            throw new AlmacenamientoException("No se pudo leer el archivo", e);
        }
    }

    @Override
    public void eliminar(String publicId) {
        try {
            Files.deleteIfExists(resolver(publicId));
        } catch (IOException e) {
            throw new AlmacenamientoException("No se pudo eliminar el archivo", e);
        }
    }

    /** Impide que una clave salga de la carpeta base (../). */
    private Path resolver(String clave) {
        Path ruta = base.resolve(clave).normalize();
        if (!ruta.startsWith(base)) {
            throw new AlmacenamientoException("Ruta de archivo inválida");
        }
        return ruta;
    }
}
