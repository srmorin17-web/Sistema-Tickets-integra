import axiosClient from './axiosClient';

const datos = (promesa) => promesa.then((r) => r.data);

/** Catálogos completos (incluye inactivos). Solo Administrador y Superadmin. */
export const obtenerCatalogosAdmin = () => datos(axiosClient.get('/admin/catalogos'));
export const obtenerTodosLosTickets = () => datos(axiosClient.get('/tickets/administracion'));

export const crearCategoria = (cuerpo) => datos(axiosClient.post('/admin/categorias', cuerpo));
export const actualizarCategoria = (id, cuerpo) => datos(axiosClient.put(`/admin/categorias/${id}`, cuerpo));

export const actualizarPrioridad = (id, cuerpo) => datos(axiosClient.put(`/admin/prioridades/${id}`, cuerpo));

export const crearArea = (cuerpo) => datos(axiosClient.post('/admin/areas-escalamiento', cuerpo));
export const actualizarArea = (id, cuerpo) => datos(axiosClient.put(`/admin/areas-escalamiento/${id}`, cuerpo));
