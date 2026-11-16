import { colorEstado, colorPrioridad, dotPrioridad, slaInfo } from '../lib/ui';

export function Badge({ children, className = '' }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${className}`}
    >
      {children}
    </span>
  );
}

export const EstadoBadge = ({ estado }) => <Badge className={colorEstado(estado)}>{estado}</Badge>;

export const PrioridadBadge = ({ prioridad }) => (
  <Badge className={colorPrioridad(prioridad)}>
    <span className={`h-1.5 w-1.5 rounded-full ${dotPrioridad(prioridad)}`} />
    {prioridad}
  </Badge>
);

export function Avatar({ initials, size = 'md' }) {
  const sz = size === 'sm' ? 'h-7 w-7 text-xs' : size === 'lg' ? 'h-12 w-12 text-base' : 'h-9 w-9 text-sm';
  return (
    <div
      className={`${sz} flex shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-sky-500 to-sky-700 font-semibold text-white`}
    >
      {initials}
    </div>
  );
}

const VARIANTES = {
  primary: 'bg-sky-600 text-white hover:bg-sky-700 active:bg-sky-800 shadow-sm',
  secondary: 'bg-slate-100 text-slate-700 hover:bg-slate-200 active:bg-slate-300',
  ghost: 'text-slate-600 hover:bg-slate-100 hover:text-slate-900',
  danger: 'bg-rose-600 text-white hover:bg-rose-700 active:bg-rose-800 shadow-sm',
  outline: 'border border-slate-300 bg-white text-slate-700 hover:bg-slate-50 active:bg-slate-100',
};
const TAMANOS = { sm: 'px-3 py-1.5 text-xs', md: 'px-4 py-2 text-sm', lg: 'px-5 py-2.5 text-sm' };

export function Button({ children, variant = 'primary', size = 'md', className = '', type = 'button', ...props }) {
  return (
    <button
      type={type}
      className={`inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-colors duration-150 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-600 disabled:cursor-not-allowed disabled:opacity-50 ${VARIANTES[variant]} ${TAMANOS[size]} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
}

export const Card = ({ children, className = '' }) => (
  <div className={`rounded-xl border border-slate-200 bg-white ${className}`}>{children}</div>
);

const ACENTOS = {
  slate: 'bg-slate-100 text-slate-600',
  blue: 'bg-blue-50 text-blue-600',
  sky: 'bg-sky-50 text-sky-600',
  emerald: 'bg-emerald-50 text-emerald-600',
  amber: 'bg-amber-50 text-amber-600',
  rose: 'bg-rose-50 text-rose-600',
  cyan: 'bg-cyan-50 text-cyan-600',
  violet: 'bg-violet-50 text-violet-600',
};

export function StatCard({ label, value, icon, accent = 'slate', sub }) {
  return (
    <Card className="p-5">
      <div className="flex items-center justify-between">
        <p className="text-sm font-medium text-slate-500">{label}</p>
        <div className={`flex h-9 w-9 items-center justify-center rounded-lg ${ACENTOS[accent]}`}>{icon}</div>
      </div>
      <p className="mt-3 text-3xl font-semibold tracking-tight text-slate-900">{value}</p>
      {sub && <p className="mt-1 text-xs text-slate-500">{sub}</p>}
    </Card>
  );
}

export function EmptyState({ icon, title, desc }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-slate-400">
        {icon}
      </div>
      <p className="text-sm font-medium text-slate-600">{title}</p>
      {desc && <p className="mt-1 max-w-sm text-xs text-slate-500">{desc}</p>}
    </div>
  );
}

export function Spinner({ label = 'Cargando...' }) {
  return (
    <div className="flex flex-col items-center justify-center py-16" role="status">
      <div className="h-8 w-8 animate-spin rounded-full border-2 border-slate-300 border-t-sky-600" />
      <p className="mt-3 text-sm text-slate-500">{label}</p>
    </div>
  );
}

const COLOR_SLA = {
  ok: 'bg-emerald-500',
  alerta: 'bg-amber-500',
  critico: 'bg-rose-500',
  vencido: 'bg-rose-600',
  pausado: 'bg-slate-400',
};

/** Barra de SLA a partir del ticket de la bandeja (fechaVencimientoSla, slaHorasSnapshot, pausado). */
export function SlaBar({ ticket }) {
  const { pct, texto, estado } = slaInfo(ticket);
  const textoRojo = estado === 'vencido' || estado === 'critico';
  return (
    <div>
      <div className="mb-1 flex items-center justify-between text-xs">
        <span className="text-slate-500">SLA</span>
        <span className={`font-medium ${textoRojo ? 'text-rose-600' : 'text-slate-600'}`}>{texto}</span>
      </div>
      <div className="h-1.5 w-full overflow-hidden rounded-full bg-slate-100">
        <div
          className={`h-full rounded-full transition-all duration-500 ${COLOR_SLA[estado]}`}
          style={{ width: `${estado === 'pausado' ? 100 : pct}%` }}
        />
      </div>
    </div>
  );
}
