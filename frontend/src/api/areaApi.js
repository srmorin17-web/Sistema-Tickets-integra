import axiosClient from './axiosClient';

const datos = (promesa) => promesa.then((r) => r.data);

/** Áreas que representa el usuario actual (vacío si no es representante). Cualquier rol puede serlo. */
export const obtenerMisAreas = () => datos(axiosClient.get('/areas/mis-areas'));

/** Tickets en estado Escalado dirigidos a las áreas del usuario. */
export const obtenerEscaladosMiArea = () => datos(axiosClient.get('/tickets/mi-area'));

/** El representante responde y el ticket vuelve al técnico. */
export const responderArea = (id, comentario) => axiosClient.post(`/tickets/${id}/respuesta-area`, { comentario });
