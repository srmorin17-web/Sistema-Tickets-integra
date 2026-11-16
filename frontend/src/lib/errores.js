/**
 * Mensajes para los errores de guardado en administración. El backend no expone el texto del error,
 * por lo que se interpreta el código de estado:
 *   400 datos inválidos, 409 nombre duplicado, 422 regla de negocio (RF-35: área con tickets activos).
 */
export function mensajeDeGuardado(error, { reglaNegocio } = {}) {
  if (!error.response) return 'No hay conexión con el servidor.';
  switch (error.response.status) {
    case 400:
      return 'Revise los datos del formulario: hay campos vacíos, demasiado largos o inválidos.';
    case 403:
      return 'No tiene permisos para realizar esta acción.';
    case 404:
      return 'El elemento ya no existe. Actualice la página.';
    case 409:
      return 'Ya existe un elemento con ese nombre.';
    case 422:
      return reglaNegocio ?? 'La operación no cumple una regla del sistema.';
    default:
      return 'No se pudieron guardar los cambios. Intente nuevamente.';
  }
}
