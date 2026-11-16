#!/bin/sh
# Prueba de la política de contraseñas y del generador de contraseñas temporales (solo requiere un JDK 17 o superior).
# Copia las dos clases del backend al paquete "p", las compila con la prueba y la ejecuta.
set -e
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
ORIGEN="$RAIZ/backend/src/main/java/com/fundacioncalma/tickets/backend/usuario/service"
TMP="$(mktemp -d)"
mkdir -p "$TMP/p"
for f in PoliticaContrasena GeneradorContrasena; do
  sed 's/^package .*/package p;/' "$ORIGEN/$f.java" > "$TMP/p/$f.java"
done
cp "$(dirname "$0")/PruebaContrasenas.java" "$TMP/p/Prueba.java"
cd "$TMP"
javac -encoding UTF-8 p/*.java
java -Dfile.encoding=UTF-8 -cp . p.Prueba
