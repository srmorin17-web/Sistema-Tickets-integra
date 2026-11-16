import axiosClient from './axiosClient';

export const obtenerDashboard = ({ inicio, fin }) =>
  axiosClient.get('/metricas/dashboard', { params: { inicio, fin } }).then((r) => r.data);
