import { useEffect, useMemo, useState } from 'react';
import { CheckCircle2, Inbox, RefreshCw, Search, UserCog, Users, UserX } from 'lucide-react';
import { obtenerSupervision, obtenerTecnicos, reasignarTicket } from '../api/asignacionApi';
import { mensajeDeError } from '../api/ticketsApi';
import { Modal } from '../components/Overlay';
import TicketDrawer from '../components/TicketDrawer';
import { Button, Card, EmptyState, EstadoBadge, PrioridadBadge, SlaBar, Spinner, StatCard } from '../components/ui';

const FILTROS = [
  { id: 'todos', etiqueta: 'Todos' },
  { id: 'sin', etiqueta: 'Sin asignar' },
];

function mensajeDeReasignacion(error) {
  if (!error.response) return 'No hay conexión con el servidor.';
  switch (error.response.status) {
    case 400:
      return 'El usuario seleccionado no es un técnico válido.';
    case 409:
      return 'El ticket ya finalizó o ya está asignado a ese técnico. Actualice la pantalla.';
    default:
      return mensajeDeError(error, 'No se pudo reasignar el ticket. Intente nuevamente.');
  }
}

function FormularioReasignacion({ ticket, tecnicos, onCerrar, onGuardado }) {
  const [tecnicoId, setTecnicoId] = useState('');
  const [motivo, setMotivo] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');

  const opciones = tecnicos.filter((t) => t.id !== ticket.tecnicoId);
  const elegido = opciones.find((t) => t.id === tecnicoId);
  const enLimite = elegido && elegido.limite > 0 && elegido.ticketsActivos >= elegido.limite;

  const guardar = async (e) => {
    e.preventDefault();
    if (!tecnicoId || enviando) return;
    setEnviando(true);
    setError('');
    try {
      await reasignarTicket(ticket.id, { tecnicoId, motivo: motivo.trim() });
      onGuardado(`Ticket ${ticket.codigo} asignado a ${elegido.nombre}.`);
    } catch (err) {
      setError(mensajeDeReasignacion(err));
      setEnviando(false);
    }
  };

  return (
    <Modal
      onClose={enviando ? () => {} : onCerrar}
      title={ticket.tecnico ? 'Reasignar ticket' : 'Asignar ticket'}
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onCerrar} disabled={enviando}>Cancelar</Button>
          <Button type="submit" form="form-reasignacion" disabled={!tecnicoId || enviando}>
            {enviando ? 'Guardando...' : ticket.tecnico ? 'Reasignar' : 'Asignar'}
          </Button>
        </div>
      }
    >
      <form id="form-reasignacion" onSubmit={guardar} className="space-y-4">
        <div className="rounded-lg bg-slate-50 px-3 py-2 text-sm">
          <p className="font-mono text-xs text-slate-500">{ticket.codigo}</p>
          <p className="font-medium text-slate-900">{ticket.asunto}</p>
          <p className="mt-0.5 text-xs text-slate-500">Técnico actual: {ticket.tecnico ?? 'sin asignar'}</p>
        </div>
        <div>
          <label htmlFor="reasig-tecnico" className="mb-1.5 block text-sm font-medium text-slate-700">Nuevo técnico</label>
          <select id="reasig-tecnico" value={tecnicoId} onChange={(e) => setTecnicoId(e.target.value)} className="input" required autoFocus>
            <option value="">Seleccione un técnico...</option>
            {opciones.map((t) => (
              <option key={t.id} value={t.id}>
                {t.nombre} ({t.ticketsActivos} {t.ticketsActivos === 1 ? 'activo' : 'activos'}{t.limite > 0 ? ` de ${t.limite}` : ''})
              </option>
            ))}
          </select>
          {enLimite && (
            <p className="mt-1.5 text-xs text-amber-700">
              Este técnico alcanzó el límite configurado. La reasignación manual lo permite, pero aumentará su carga.
            </p>
          )}
          {opciones.length === 0 && <p className="mt-1.5 text-xs text-slate-500">No hay otros técnicos disponibles.</p>}
        </div>
        <div>
          <label htmlFor="reasig-motivo" className="mb-1.5 block text-sm font-medium text-slate-700">Motivo (opcional)</label>
          <textarea id="reasig-motivo" value={motivo} onChange={(e) => setMotivo(e.target.value)} rows={3} maxLength={500}
            placeholder="Por ejemplo: caso especial, ausencia del técnico, especialidad requerida..." className="input resize-none" />
        </div>
        {error && <p role="alert" className="text-sm text-rose-600">{error}</p>}
      </form>
    </Modal>
  );
}

export default function Asignacion() {
  const [resultado, setResultado] = useState({ clave: null, tecnicos: [], tickets: [], error: '' });
  const [recarga, setRecarga] = useState(0);
  const [busqueda, setBusqueda] = useState('');
  const [filtro, setFiltro] = useState('todos');
  const [reasignando, setReasignando] = useState(null);
  const [detalle, setDetalle] = useState(null);
  const [aviso, setAviso] = useState('');

  const clave = String(recarga);

  useEffect(() => {
    let activo = true;
    Promise.all([obtenerTecnicos(), obtenerSupervision()])
      .then(([tecnicos, tickets]) => activo && setResultado({ clave, tecnicos, tickets, error: '' }))
      .catch((e) =>
        activo && setResultado({ clave, tecnicos: [], tickets: [], error: mensajeDeError(e, 'No se pudo cargar la información de asignación.') })
      );
    return () => {
      activo = false;
    };
  }, [clave]);

  const cargando = resultado.clave !== clave;
  const { tecnicos, tickets, error } = resultado;
  const limite = tecnicos[0]?.limite ?? 0;
  const sinAsignar = tickets.filter((t) => !t.tecnicoId).length;

  const filtrados = useMemo(() => {
    const q = busqueda.trim().toLowerCase();
    return tickets.filter(
      (t) =>
        (filtro === 'todos' || !t.tecnicoId) &&
        (!q || [t.codigo, t.asunto, t.solicitante, t.tecnico].some((c) => c?.toLowerCase().includes(q)))
    );
  }, [tickets, busqueda, filtro]);

  const guardado = (mensaje) => {
    setReasignando(null);
    setAviso(mensaje);
    setRecarga((n) => n + 1);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Asignación de tickets</h1>
          <p className="mt-1 text-sm text-slate-500">Carga de cada técnico y reasignación manual de casos especiales.</p>
        </div>
        <Button variant="outline" size="sm" onClick={() => setRecarga((n) => n + 1)} disabled={cargando}>
          <RefreshCw size={14} className={cargando ? 'animate-spin' : ''} />
          {cargando ? 'Actualizando...' : 'Actualizar'}
        </Button>
      </div>

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard label="Tickets activos" value={tickets.length} icon={<Inbox size={18} />} accent="sky" />
        <StatCard label="Sin asignar" value={sinAsignar} icon={<UserX size={18} />} accent={sinAsignar > 0 ? 'amber' : 'slate'} />
        <StatCard label="Técnicos" value={tecnicos.length} icon={<Users size={18} />} accent="emerald" />
        <StatCard label="Límite por técnico" value={limite > 0 ? limite : 'Sin límite'} icon={<UserCog size={18} />} accent="violet" />
      </div>

      {aviso && (
        <div role="status" className="flex items-center gap-2 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700 ring-1 ring-emerald-200">
          <CheckCircle2 size={15} /> {aviso}
        </div>
      )}

      {error && (
        <div role="alert" className="flex items-center justify-between gap-3 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200">
          <span>{error}</span>
          <Button size="sm" variant="outline" onClick={() => setRecarga((n) => n + 1)}>Reintentar</Button>
        </div>
      )}

      {sinAsignar > 0 && (
        <div role="status" className="rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-800 ring-1 ring-amber-200">
          Hay {sinAsignar} {sinAsignar === 1 ? 'ticket sin técnico' : 'tickets sin técnico'}: ningún técnico estaba disponible al crearlos
          (sin técnicos registrados o todos en su límite). Asígnelos manualmente; su SLA ya está corriendo.
        </div>
      )}

      <Card className="p-5">
        <h2 className="text-sm font-semibold text-slate-900">Carga por técnico</h2>
        {cargando && tecnicos.length === 0 ? (
          <Spinner label="Cargando técnicos..." />
        ) : tecnicos.length === 0 ? (
          <p className="mt-3 text-sm text-slate-500">No hay técnicos registrados.</p>
        ) : (
          <ul className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {tecnicos.map((t) => {
              const pct = t.limite > 0 ? Math.min(100, (t.ticketsActivos / t.limite) * 100) : null;
              return (
                <li key={t.id} className="rounded-lg border border-slate-200 p-3">
                  <div className="flex items-center justify-between gap-2">
                    <span className="truncate text-sm font-medium text-slate-900">{t.nombre}</span>
                    <span className="shrink-0 text-sm font-semibold text-slate-700">
                      {t.ticketsActivos}{t.limite > 0 ? ` / ${t.limite}` : ''}
                    </span>
                  </div>
                  {pct !== null && (
                    <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-slate-100" role="presentation">
                      <div className={`h-full rounded-full ${pct >= 100 ? 'bg-rose-500' : pct >= 75 ? 'bg-amber-500' : 'bg-emerald-500'}`} style={{ width: `${pct}%` }} />
                    </div>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </Card>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative max-w-sm flex-1">
          <label htmlFor="busqueda-asig" className="sr-only">Buscar ticket</label>
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input id="busqueda-asig" value={busqueda} onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por código, asunto, solicitante o técnico" className="input py-2 pl-10" />
        </div>
        <div className="flex gap-1 rounded-lg border border-slate-200 bg-white p-1" role="group" aria-label="Filtrar tickets">
          {FILTROS.map((f) => (
            <button key={f.id} type="button" aria-pressed={filtro === f.id} onClick={() => setFiltro(f.id)}
              className={`rounded-md px-3 py-1.5 text-xs font-medium transition-colors ${
                filtro === f.id ? 'bg-sky-600 text-white' : 'text-slate-600 hover:bg-slate-100'
              }`}>
              {f.etiqueta}
            </button>
          ))}
        </div>
      </div>

      <Card className="overflow-hidden">
        {cargando && tickets.length === 0 ? (
          <Spinner label="Cargando tickets..." />
        ) : filtrados.length === 0 ? (
          <EmptyState icon={<Inbox size={22} />}
            title={tickets.length === 0 ? 'No hay tickets activos' : 'Ningún ticket coincide con el filtro'}
            desc={tickets.length === 0 ? 'Los tickets nuevos aparecerán aquí, ordenados por tiempo restante de SLA.' : 'Pruebe con otra búsqueda o seleccione "Todos".'} />
        ) : (
          <ul className="divide-y divide-slate-100">
            {filtrados.map((t) => (
              <li key={t.id} className="flex items-center gap-4 px-5 py-4">
                <button type="button" onClick={() => setDetalle(t.id)}
                  className="min-w-0 flex-1 rounded text-left focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-600">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-mono text-xs font-medium text-slate-500">{t.codigo}</span>
                    <EstadoBadge estado={t.estado} />
                    <PrioridadBadge prioridad={t.prioridad} />
                  </div>
                  <p className="mt-1 truncate text-sm font-medium text-slate-900">{t.asunto}</p>
                  <p className="mt-0.5 text-xs text-slate-500">
                    {t.solicitante} ·{' '}
                    {t.tecnico ? <>atiende {t.tecnico}</> : <span className="font-medium text-amber-700">sin técnico asignado</span>}
                  </p>
                </button>
                <div className="hidden w-40 shrink-0 md:block"><SlaBar ticket={t} /></div>
                <Button size="sm" variant={t.tecnicoId ? 'outline' : 'primary'} onClick={() => { setAviso(''); setReasignando(t); }}
                  aria-label={`${t.tecnicoId ? 'Reasignar' : 'Asignar'} ${t.codigo}`}>
                  <UserCog size={14} /> {t.tecnicoId ? 'Reasignar' : 'Asignar'}
                </Button>
              </li>
            ))}
          </ul>
        )}
      </Card>

      {reasignando && (
        <FormularioReasignacion key={reasignando.id} ticket={reasignando} tecnicos={tecnicos}
          onCerrar={() => setReasignando(null)} onGuardado={guardado} />
      )}
      {detalle && (
        <TicketDrawer key={detalle} ticketId={detalle} onClose={() => setDetalle(null)} onChanged={() => setRecarga((n) => n + 1)} />
      )}
    </div>
  );
}
