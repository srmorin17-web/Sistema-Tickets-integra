#!/bin/sh
# Prueba del cliente REST de RR. HH. y de la caché, contra un RR. HH. SIMULADO (JDK 17 o superior + librerías Jackson 2).
# Las librerías Jackson se buscan en JACKSON_DIR (por defecto /usr/share/java, paquetes libjackson2-*-java de Debian/Ubuntu)
# o, si no están allí, en el repositorio local de Maven (~/.m2).
set -e
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
ORIGEN="$RAIZ/backend/src/main/java/com/fundacioncalma/tickets/backend/rrhh/cliente"
JD="${JACKSON_DIR:-/usr/share/java}"
buscar() { ls "$JD"/$1*.jar 2>/dev/null | head -1 || true; }
CORE="$(buscar jackson-core)"; DB="$(buscar jackson-databind)"; ANN="$(buscar jackson-annotations)"
if [ -z "$CORE" ] || [ -z "$DB" ] || [ -z "$ANN" ]; then
  CORE="$(find "$HOME/.m2" -name 'jackson-core-2*.jar' 2>/dev/null | head -1)"
  DB="$(find "$HOME/.m2" -name 'jackson-databind-2*.jar' 2>/dev/null | head -1)"
  ANN="$(find "$HOME/.m2" -name 'jackson-annotations-2*.jar' 2>/dev/null | head -1)"
fi
[ -n "$CORE" ] && [ -n "$DB" ] && [ -n "$ANN" ] || { echo "No se encontraron las librerías Jackson 2. Defina JACKSON_DIR."; exit 1; }
CP="$CORE:$DB:$ANN"
TMP="$(mktemp -d)"
DEST="$TMP/src/com/fundacioncalma/tickets/backend/rrhh/cliente"
mkdir -p "$DEST" "$TMP/out"
cp "$ORIGEN"/*.java "$DEST/"
cp "$(dirname "$0")/PruebaRrhh.java" "$DEST/"
javac -encoding UTF-8 -cp "$CP" -d "$TMP/out" "$DEST"/*.java
java -Dfile.encoding=UTF-8 -cp "$TMP/out:$CP" com.fundacioncalma.tickets.backend.rrhh.cliente.PruebaRrhh
