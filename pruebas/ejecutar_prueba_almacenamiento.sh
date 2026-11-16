#!/bin/sh
# Prueba del módulo de adjuntos (validador de archivos, almacenamiento local y adaptador de Cloudinary).
# Copia las clases de Java del paquete "almacenamiento" quitándoles las anotaciones de Spring, las compila con una
# prueba que levanta un servidor Cloudinary SIMULADO (valida la firma recalculándola de forma independiente) y la ejecuta.
# Requiere solo un JDK 17 o superior. No se conecta a Cloudinary real.
set -e
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
ORIGEN="$RAIZ/backend/src/main/java/com/fundacioncalma/tickets/backend/almacenamiento"
TMP="$(mktemp -d)"
mkdir -p "$TMP/alm"
for f in "$ORIGEN"/*.java; do
  sed -E 's/^package .*/package alm;/; /^import org\.springframework/d; /^@Component/d; /^@ConditionalOnProperty/d; s/@Value\("[^"]*"\) //g; s/^import com\.fundacioncalma\.tickets\.backend\.almacenamiento\.ArchivoInvalidoException\.Motivo;/import alm.ArchivoInvalidoException.Motivo;/' "$f" > "$TMP/alm/$(basename "$f")"
done
cp "$(dirname "$0")/PruebaAlmacenamiento.java" "$TMP/alm/Prueba.java"
cd "$TMP"
javac -encoding UTF-8 alm/*.java
java -Dfile.encoding=UTF-8 -cp . alm.Prueba
