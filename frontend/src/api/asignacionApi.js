import axiosClient from './axiosClient';

const datos = (promesa) => promesa.then((r) => r.data);

/** Técnicos con su carga actual y el límite configurado (0 = sin límite). Supervisor, Administrador y Superadmin. */
export const obtenerTecnicos = () => datos(axiosClient.get('/tecnicos'));

/** Todos los tickets activos, incluidos los que aún no tienen técnico. */
export const obtenerSupervision = () => datos(axiosClient.get('/tickets/supervision'));

export const reasignarTicket = (id, { tecnicoId, motivo }) =>
  datos(axiosClient.put(`/tickets/${id}/asignacion`, { tecnicoId, motivo: motivo || null }));
