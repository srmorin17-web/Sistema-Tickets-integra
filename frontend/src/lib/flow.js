/**
 * Flujo de estados que ofrece la interfaz. El backend solo valida que Resuelto lleve comentario;
 * las transiciones permitidas se aplican aquí.
 *
 * Recibido -> En revisión -> Requiere información -> En revisión (reanuda el SLA)
 *                         -> Resuelto
 *                         -> Escalado (acción "Escalar", con área y motivo) -> En revisión (reanuda el SLA)
 *                                                                          -> Resuelto
 *
 * "Cerrado" lo asigna el backend automáticamente a las 72 h de resuelto.
 * Reanudar desde Escalado requiere la corrección del Bloque 3 en TicketService.actualizarEstado.
 */
export const TRANSICIONES = {
  'Recibido': ['En revisión'],
  'En revisión': ['Requiere información', 'Resuelto'],
  'Requiere información': ['En revisión'],
  'Escalado': ['En revisión', 'Resuelto'],
  'Resuelto': [],
  'Cerrado': [],
};

export const ESTADOS_ESCALABLES = ['En revisión'];

/** Destinos que exigen que el técnico escriba un comentario. */
export const COMENTARIO_OBLIGATORIO = ['Resuelto', 'Requiere información'];

export function etiquetaAccion(desde, hacia) {
  if (hacia === 'En revisión') return desde === 'Recibido' ? 'Iniciar revisión' : 'Reanudar revisión';
  if (hacia === 'Requiere información') return 'Solicitar información';
  if (hacia === 'Resuelto') return 'Marcar como resuelto';
  return hacia;
}

/**
 * El backend solo registra la auditoría si el comentario no es nulo,
 * por lo que las transiciones sin comentario del usuario envían uno por defecto.
 */
export function comentarioPorDefecto(desde, hacia) {
  if (hacia === 'En revisión') {
    if (desde === 'Recibido') return 'El ticket pasó a revisión técnica.';
    if (desde === 'Escalado') return 'El área destino respondió; se reanuda la revisión.';
    return 'Se recibió la información solicitada; se reanuda la revisión.';
  }
  return `Cambio de estado a ${hacia}.`;
}
