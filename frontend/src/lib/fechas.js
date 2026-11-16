// Fechas en formato local "YYYY-MM-DD" (valor de <input type="date">), sin conversión a UTC.
const pad = (n) => String(n).padStart(2, '0');

export const aInputFecha = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

export function rangoUltimosDias(dias) {
  const hoy = new Date();
  const desde = new Date(hoy);
  desde.setDate(desde.getDate() - dias);
  return { desde: aInputFecha(desde), hasta: aInputFecha(hoy) };
}

// El backend recibe LocalDateTime ISO. Se usa el día completo para que las consultas
// repetidas compartan la misma clave en el caché del dashboard.
export const inicioDelDia = (fecha) => `${fecha}T00:00:00`;
export const finDelDia = (fecha) => `${fecha}T23:59:59`;
