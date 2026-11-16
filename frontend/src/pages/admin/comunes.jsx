import { Badge } from '../../components/ui';

export const EstadoActivo = ({ activo }) => (
  <Badge className={activo ? 'bg-emerald-50 text-emerald-700 ring-emerald-200' : 'bg-slate-100 text-slate-500 ring-slate-200'}>
    {activo ? 'Activa' : 'Inactiva'}
  </Badge>
);

/** Encabezado de cada panel: título, nota y acción principal. */
export function CabeceraPanel({ titulo, nota, accion }) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-200 px-5 py-4">
      <div>
        <h2 className="text-sm font-semibold text-slate-900">{titulo}</h2>
        {nota && <p className="mt-0.5 max-w-xl text-xs text-slate-500">{nota}</p>}
      </div>
      {accion}
    </div>
  );
}

export function Campo({ id, etiqueta, ayuda, children }) {
  return (
    <div>
      <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-slate-700">{etiqueta}</label>
      {children}
      {ayuda && <p className="mt-1 text-xs text-slate-500">{ayuda}</p>}
    </div>
  );
}
