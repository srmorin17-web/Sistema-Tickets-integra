import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { CheckCircle2, Clock, Inbox, MessageSquare, PlusCircle, RefreshCw, Search } from 'lucide-react';
import { mensajeDeError, obtenerMisTickets } from '../api/ticketsApi';
import TicketDrawer from '../components/TicketDrawer';
import { Button, Card, EmptyState, EstadoBadge, PrioridadBadge, Spinner, StatCard } from '../components/ui';
import { timeAgo } from '../lib/ui';

const REFRESCO_MS = 60_000;
const FINALES = ['Resuelto', 'Cerrado'];
const FILTROS = [
  { id: 'todos', etiqueta: 'Todos' },
  { id: 'curso', etiqueta: 'En curso' },
  { id: 'finalizados', etiqueta: 'Finalizados' },
];

export default function MisTickets() {
  const [tickets, setTickets] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [actualizando, setActualizando] = useState(false);
  const [error, setError] = useState('');
  const [recarga, setRecarga] = useState(0);
  const [busqueda, setBusqueda] = useState('');
  const [filtro, setFiltro] = useState('todos');
  const [abierto, setAbierto] = useState(null);

  useEffect(() => {
    let activo = true;
    obtenerMisTickets()
      .then((datos) => {
        if (!activo) return;
        setTickets(datos);
        setError('');
      })
      .catch((e) => {
        if (activo) setError(mensajeDeError(e, 'No se pudieron cargar sus tickets.'));
      })
      .finally(() => {
        if (!activo) return;
        setCargando(false);
        setActualizando(false);
      });
    return () => {
      activo = false;
    };
  }, [recarga]);

  useEffect(() => {
    const id = setInterval(() => setRecarga((n) => n + 1), REFRESCO_MS);
    return () => clearInterval(id);
  }, []);

  const refrescar = () => {
    setActualizando(true);
    setRecarga((n) => n + 1);
  };

  const enCurso = tickets.filter((t) => !FINALES.includes(t.estado));
  const esperandoInfo = tickets.filter((t) => t.estado === 'Requiere información').length;
  const finalizados = tickets.length - enCurso.length;

  const filtrados = useMemo(() => {
    const q = busqueda.trim().toLowerCase();
    return tickets.filter((t) => {
      const final = FINALES.includes(t.estado);
      if (filtro === 'curso' && final) return false;
      if (filtro === 'finalizados' && !final) return false;
      return !q || [t.asunto, t.codigo, t.categoria].some((c) => c?.toLowerCase().includes(q));
    });
  }, [tickets, busqueda, filtro]);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Mis tickets</h1>
          <p className="mt-1 text-sm text-slate-500">Seguimiento de las solicitudes que ha registrado.</p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" onClick={refrescar} disabled={actualizando}>
            <RefreshCw size={14} className={actualizando ? 'animate-spin' : ''} />
            {actualizando ? 'Actualizando...' : 'Actualizar'}
          </Button>
          <Link
            to="/nuevo-ticket"
            className="inline-flex items-center gap-2 rounded-lg bg-sky-600 px-3 py-1.5 text-xs font-medium text-white shadow-sm transition-colors hover:bg-sky-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-600"
          >
            <PlusCircle size={14} /> Nuevo ticket
          </Link>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard label="Total" value={tickets.length} icon={<Inbox size={18} />} accent="sky" />
        <StatCard label="En curso" value={enCurso.length} icon={<Clock size={18} />} accent="amber" />
        <StatCard label="Esperan su respuesta" value={esperandoInfo} icon={<MessageSquare size={18} />} accent="violet" />
        <StatCard label="Finalizados" value={finalizados} icon={<CheckCircle2 size={18} />} accent="emerald" />
      </div>

      {error && (
        <div role="alert" className="flex items-center justify-between gap-3 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200">
          <span>{error}</span>
          <Button size="sm" variant="outline" onClick={refrescar}>Reintentar</Button>
        </div>
      )}

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative max-w-sm flex-1">
          <label htmlFor="busqueda-mis" className="sr-only">Buscar ticket</label>
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input id="busqueda-mis" value={busqueda} onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por código, asunto o categoría" className="input py-2 pl-10" />
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
        {cargando ? (
          <Spinner label="Cargando sus tickets..." />
        ) : filtrados.length === 0 ? (
          <EmptyState
            icon={<Inbox size={22} />}
            title={tickets.length === 0 ? 'Aún no ha registrado tickets' : 'Ningún ticket coincide con el filtro'}
            desc={tickets.length === 0 ? 'Use "Nuevo ticket" para reportar una incidencia o solicitar soporte.' : 'Pruebe con otra búsqueda o seleccione "Todos".'}
          />
        ) : (
          <ul className="divide-y divide-slate-100">
            {filtrados.map((t) => (
              <li key={t.id}>
                <button type="button" onClick={() => setAbierto(t.id)}
                  className="flex w-full items-center gap-4 px-5 py-4 text-left transition-colors hover:bg-slate-50 focus-visible:bg-slate-50 focus-visible:outline focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-sky-600">
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="font-mono text-xs font-medium text-slate-500">{t.codigo}</span>
                      <EstadoBadge estado={t.estado} />
                      <PrioridadBadge prioridad={t.prioridad} />
                    </div>
                    <p className="mt-1 truncate text-sm font-medium text-slate-900">{t.asunto}</p>
                    <p className="mt-0.5 text-xs text-slate-500">
                      {t.categoria} · creado {timeAgo(t.fechaCreacion)}
                    </p>
                  </div>
                  <div className="hidden shrink-0 text-right sm:block">
                    <p className="text-xs text-slate-500">Atiende</p>
                    <p className="text-sm font-medium text-slate-700">{t.tecnicoAsignado ?? 'Sin asignar'}</p>
                  </div>
                </button>
              </li>
            ))}
          </ul>
        )}
      </Card>

      {abierto && <TicketDrawer key={abierto} ticketId={abierto} onClose={() => setAbierto(null)} vistaSolicitante />}
    </div>
  );
}
