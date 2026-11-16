// Colores y utilidades visuales. Las claves coinciden con los nombres que devuelve el backend.

export const ESTADO_COLOR = {
  'Recibido': 'bg-slate-100 text-slate-700 ring-slate-200',
  'En revisión': 'bg-sky-50 text-sky-700 ring-sky-200',
  'Requiere información': 'bg-amber-50 text-amber-700 ring-amber-200',
  'Escalado': 'bg-violet-50 text-violet-700 ring-violet-200',
  'Resuelto': 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  'Cerrado': 'bg-slate-100 text-slate-500 ring-slate-200',
};

export const PRIORIDAD_COLOR = {
  'Crítica': 'bg-rose-50 text-rose-700 ring-rose-200',
  'Alta': 'bg-orange-50 text-orange-700 ring-orange-200',
  'Media': 'bg-amber-50 text-amber-700 ring-amber-200',
  'Baja': 'bg-cyan-50 text-cyan-700 ring-cyan-200',
};

export const PRIORIDAD_DOT = {
  'Crítica': 'bg-rose-500',
  'Alta': 'bg-orange-500',
  'Media': 'bg-amber-500',
  'Baja': 'bg-cyan-500',
};

const FALLBACK_COLOR = 'bg-slate-100 text-slate-700 ring-slate-200';
export const colorEstado = (e) => ESTADO_COLOR[e] ?? FALLBACK_COLOR;
export const colorPrioridad = (p) => PRIORIDAD_COLOR[p] ?? FALLBACK_COLOR;
export const dotPrioridad = (p) => PRIORIDAD_DOT[p] ?? 'bg-slate-400';

export function timeAgo(iso) {
  const m = Math.floor((Date.now() - new Date(iso).getTime()) / 60000);
  if (m < 1) return 'hace instantes';
  if (m < 60) return `hace ${m} min`;
  const h = Math.floor(m / 60);
  if (h < 24) return `hace ${h} h`;
  const d = Math.floor(h / 24);
  if (d < 30) return `hace ${d} d`;
  const mo = Math.floor(d / 30);
  return `hace ${mo} ${mo > 1 ? 'meses' : 'mes'}`;
}

export function formatDate(iso) {
  return new Date(iso).toLocaleString('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/**
 * Estado visual del SLA a partir de lo que entrega /tickets/bandeja:
 * fechaVencimientoSla, slaHorasSnapshot y pausado.
 * El backend no envía la fecha de inicio, por lo que el avance se aproxima
 * con las horas de SLA registradas al crear el ticket.
 */
export function slaInfo({ fechaVencimientoSla, slaHorasSnapshot, pausado }) {
  if (pausado) return { pct: 0, texto: 'Pausado', estado: 'pausado' };
  if (!fechaVencimientoSla) return { pct: 0, texto: 'Sin SLA', estado: 'ok' };

  const restanteMs = new Date(fechaVencimientoSla).getTime() - Date.now();
  if (restanteMs < 0) return { pct: 100, texto: 'Vencido', estado: 'vencido' };

  const restanteH = restanteMs / 3600_000;
  const totalH = slaHorasSnapshot > 0 ? slaHorasSnapshot : null;
  const pct = totalH ? Math.min(100, Math.max(0, (1 - restanteH / totalH) * 100)) : 0;

  const h = Math.floor(restanteH);
  const m = Math.floor((restanteMs % 3600_000) / 60000);
  const texto = h > 0 ? `${h} h ${m} min` : `${m} min`;

  let estado = 'ok';
  if (pct > 90) estado = 'critico';
  else if (pct > 70) estado = 'alerta';
  return { pct, texto, estado };
}
