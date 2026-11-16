// Política de adjuntos. Debe coincidir con el backend (ValidadorArchivo y application.yml);
// el backend vuelve a validar todo, esto solo evita subidas que seguro serán rechazadas.
export const MAX_BYTES = 5 * 1024 * 1024;
export const MAX_ADJUNTOS = 5;

export const EXTENSIONES = [
  'csv', 'doc', 'docx', 'gif', 'jpeg', 'jpg', 'log', 'pdf', 'png', 'ppt', 'pptx', 'txt', 'webp', 'xls', 'xlsx',
];
export const ACCEPT = EXTENSIONES.map((e) => `.${e}`).join(',');

export function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** Devuelve el motivo del rechazo, o null si el archivo es aceptable. */
export function validarArchivo(archivo, yaSeleccionados = 0) {
  if (yaSeleccionados >= MAX_ADJUNTOS) return `Solo se permiten ${MAX_ADJUNTOS} archivos por ticket.`;
  if (archivo.size === 0) return `"${archivo.name}" está vacío.`;
  if (archivo.size > MAX_BYTES) return `"${archivo.name}" supera el máximo de ${formatBytes(MAX_BYTES)}.`;
  const extension = archivo.name.includes('.') ? archivo.name.split('.').pop().toLowerCase() : '';
  if (!EXTENSIONES.includes(extension)) {
    return `"${archivo.name}": tipo no permitido. Formatos aceptados: ${EXTENSIONES.join(', ')}.`;
  }
  return null;
}

/** Mensaje según el código de estado con que el backend rechazó el archivo. */
export function mensajeDeAdjunto(error) {
  if (!error.response) return 'No hay conexión con el servidor.';
  switch (error.response.status) {
    case 400:
      return 'El archivo no es válido (vacío o sin nombre y extensión).';
    case 403:
      return 'No tiene permisos para esta acción.';
    case 404:
      return 'El ticket o el archivo ya no existe.';
    case 409:
      return 'No se pudo: el ticket ya finalizó o alcanzó el máximo de archivos.';
    case 410:
      return 'El archivo fue eliminado por la política de retención.';
    case 413:
      return `El archivo supera el máximo de ${formatBytes(MAX_BYTES)}.`;
    case 415:
      return 'Tipo de archivo no permitido, o su contenido no corresponde a la extensión.';
    case 502:
      return 'El almacenamiento de archivos no está disponible. Intente más tarde.';
    default:
      return 'No se pudo completar la operación con el archivo.';
  }
}
