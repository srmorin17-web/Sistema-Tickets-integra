import axiosClient from './axiosClient';

const EXTENSION = { excel: 'xlsx', pdf: 'pdf' };

export async function obtenerReporte({ inicio, fin, categoriaId, tecnicoId }) {
  const params = { inicio, fin };
  if (categoriaId) params.categoriaId = categoriaId;
  if (tecnicoId) params.tecnicoId = tecnicoId;
  const { data } = await axiosClient.get('/reportes/tickets', { params });
  return data;
}

/**
 * Descarga el reporte generado por el backend (GET /reportes/tickets/{excel|pdf}).
 * La petición lleva el token del interceptor, por eso no puede ser un enlace directo.
 */
export async function descargarReporte(formato, { inicio, fin, categoriaId, tecnicoId }) {
  const params = { inicio, fin };
  if (categoriaId) params.categoriaId = categoriaId;
  if (tecnicoId) params.tecnicoId = tecnicoId;

  const { data } = await axiosClient.get(`/reportes/tickets/${formato}`, {
    params,
    responseType: 'blob',
  });

  const url = URL.createObjectURL(data);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = `Reporte_Tickets_${inicio.slice(0, 10)}_${fin.slice(0, 10)}.${EXTENSION[formato]}`;
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
  URL.revokeObjectURL(url);
}
