import axiosClient from './axiosClient';

const datos = (promesa) => promesa.then((r) => r.data);

export const obtenerCatalogos = () => datos(axiosClient.get('/catalogos'));
export const obtenerBandeja = () => datos(axiosClient.get('/tickets/bandeja'));
export const obtenerMisTickets = () => datos(axiosClient.get('/tickets/mis-tickets'));
export const obtenerTicket = (id) => datos(axiosClient.get(`/tickets/${id}`));

/** La prioridad no se envía: el sistema la define según la categoría. */
export const crearTicket = ({ asunto, descripcion, categoriaId }) =>
  datos(axiosClient.post('/tickets', { asunto, descripcion, categoriaId }));

export const cambiarEstado = (id, { nuevoEstadoId, comentario }) =>
  datos(axiosClient.put(`/tickets/${id}/estado`, { nuevoEstadoId, comentario, esInterno: false }));

export const responderSolicitudInformacion = (id, comentario) =>
  datos(axiosClient.post(`/tickets/${id}/informacion`, { comentario }));

export const escalarTicket = (id, { areaEscalamientoId, motivo }) =>
  datos(axiosClient.post(`/tickets/${id}/escalar`, { areaEscalamientoId, motivo }));

/**
 * El backend no devuelve mensajes de error legibles (responde con el 500 genérico de Spring),
 * por lo que el mensaje se arma según el código de estado.
 */
export function mensajeDeError(error, porDefecto) {
  if (!error.response) return 'No hay conexión con el servidor.';
  switch (error.response.status) {
    case 403:
      return 'No tiene permisos para realizar esta acción.';
    case 404:
      return 'El ticket ya no existe.';
    default:
      return porDefecto;
  }
}

/** El solicitante deja un comentario (opcional, una sola vez) sobre un ticket resuelto. */
export const enviarRevision = (id, comentario) =>
  datos(axiosClient.post(`/tickets/${id}/revision`, { comentario }));

/** El solicitante confirma la solución y cierra el ticket inmediatamente. */
export const confirmarCierre = (id) =>
  datos(axiosClient.post(`/tickets/${id}/confirmar-cierre`));

export const solicitarReapertura = (id, motivo) =>
  datos(axiosClient.post(`/tickets/${id}/reabrir`, { motivo }));
