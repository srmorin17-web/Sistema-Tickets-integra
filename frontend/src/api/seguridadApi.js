import axiosClient from './axiosClient';

const datos = (promesa) => promesa.then((r) => r.data);

/** Avisa al backend que el usuario intentó abrir una pantalla que su rol no permite. */
export const reportarVistaDenegada = (ruta) => axiosClient.post('/seguridad/intentos-acceso', { ruta });

/** Solo Superadmin: últimos inicios de sesión y últimos accesos denegados. */
export const obtenerAccesos = (limite = 200) => datos(axiosClient.get('/auditoria/accesos', { params: { limite } }));
export const obtenerIntentos = (limite = 200) => datos(axiosClient.get('/auditoria/intentos', { params: { limite } }));
export const obtenerGestionUsuarios = (limite = 200) => datos(axiosClient.get('/auditoria/usuarios', { params: { limite } }));
