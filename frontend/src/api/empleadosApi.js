import axiosClient from './axiosClient';

/** Empleados de RR. HH. cuyo nombre o correo contiene el texto. Solo Administrador y Superadmin. */
export const buscarEmpleados = (q, limite = 15) =>
  axiosClient.get('/empleados', { params: { q, limite } }).then((r) => r.data);
