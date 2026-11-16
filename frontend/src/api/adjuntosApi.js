import axiosClient from './axiosClient';

export const listarAdjuntos = (ticketId) => axiosClient.get(`/tickets/${ticketId}/adjuntos`).then((r) => r.data);

/** Sube un archivo. `onProgreso` recibe un número de 0 a 100. */
export function subirAdjunto(ticketId, archivo, onProgreso) {
  const formulario = new FormData();
  formulario.append('archivo', archivo);
  return axiosClient
    .post(`/tickets/${ticketId}/adjuntos`, formulario, {
      // Sin Content-Type fijo: el navegador agrega el multipart con su "boundary"
      headers: { 'Content-Type': undefined },
      onUploadProgress: (e) => e.total && onProgreso?.(Math.round((e.loaded / e.total) * 100)),
    })
    .then((r) => r.data);
}

/** Descarga el archivo a través del backend (que verifica permisos) y lo guarda con su nombre original. */
export async function descargarAdjunto(adjunto) {
  const { data } = await axiosClient.get(`/adjuntos/${adjunto.id}/descarga`, { responseType: 'blob' });
  const url = URL.createObjectURL(data);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = adjunto.nombre;
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
  URL.revokeObjectURL(url);
}

export const eliminarAdjunto = (adjuntoId) => axiosClient.delete(`/adjuntos/${adjuntoId}`);
