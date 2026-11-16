import axiosClient from './axiosClient';

export const obtenerEstadoSincronizacionRrhh = () =>
  axiosClient.get('/admin/rrhh-sync').then((r) => r.data);

export const sincronizarRrhhAhora = () =>
  axiosClient.post('/admin/rrhh-sync').then((r) => r.data);
