import { useEffect, useMemo, useState } from 'react';
import { AlertTriangle, Inbox, PauseCircle, RefreshCw, Search, Siren } from 'lucide-react';
import { mensajeDeError, obtenerBandeja } from '../api/ticketsApi';
import TicketDrawer from '../components/TicketDrawer';
import { Button, Card, EmptyState, EstadoBadge, PrioridadBadge, SlaBar, Spinner, StatCard } from '../components/ui';
import { slaInfo } from '../lib/ui';

const REFRESCO_MS = 60_000;

export default function BandejaTecnico() {
  const [tickets, setTickets] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [actualizando, setActualizando] = useState(false);
  const [error, setError] = useState('');
  const [recarga, setRecarga] = useState(0);
  const [busqueda, setBusqueda] = useState('');
  const [filtroEstado, setFiltroEstado] = useState('Todos');
  const [abierto, setAbierto] = useState(null);

  useEffect(() => {
    let activo = true;
    obtenerBandeja()
      .then((datos) => {
        if (!activo) return;
        setTickets(datos);
        setError('');
      })
      .catch((e) => {
        if (activo) setError(mensajeDeError(e, 'No se pudo cargar la bandeja de tickets.'));
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

  // Actualización periódica para mantener al día los SLA y las nuevas asignaciones
  useEffect(() => {
    const id = setInterval(() => setRecarga((n) => n + 1), REFRESCO_MS);
    return () => clearInterval(id);
  }, []);

  const refrescar = () => {
    setActualizando(true);
    setRecarga((n) => n + 1);
  };

  const estadosPresentes = useMemo(() => ['Todos', ...new Set(tickets.map((t) => t.estado))], [tickets]);

  const filtrados = useMemo(() => {
    const q = busqueda.trim().toLowerCase();
    return tickets.filter(
      (t) =>
        (filtroEstado === 'Todos' || t.estado === filtroEstado) &&
        (!q || [t.asunto, t.codigo, t.solicitante].some((c) => c?.toLowerCase().includes(q)))
    );
  }, [tickets, busqueda, filtroEstado]);

  const criticos = tickets.filter((t) => t.prioridad === 'Crítica').length;
  const vencidos = tickets.filter((t) => slaInfo(t).estado === 'vencido').length;
  const pausados = tickets.filter((t) => t.pausado).length;

  // Si el filtro activo deja de existir tras una actualización, se vuelve a "Todos"
  const filtroVigente = estadosPresentes.includes(filtroEstado) ? filtroEstado : 'Todos';

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Bandeja de tickets</h1>
          <p className="mt-1 text-sm text-slate-500">Tickets activos asignados a usted, ordenados por tiempo restante de SLA (los pausados, al final).</p>
        </div>
        <Button variant="outline" size="sm" onClick={refrescar} disabled={actualizando}>
          <RefreshCw size={14} className={actualizando ? 'animate-spin' : ''} />
          {actualizando ? 'Actualizando...' : 'Actualizar'}
        </Button>
      </div>

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard label="Asignados" value={tickets.length} icon={<Inbox size={18} />} accent="sky" />
        <StatCard label="Prioridad crítica" value={criticos} icon={<Siren size={18} />} accent="rose" />
        <StatCard label="SLA vencido" value={vencidos} icon={<AlertTriangle size={18} />} accent="amber" />
        <StatCard label="SLA en pausa" value={pausados} icon={<PauseCircle size={18} />} accent="slate" />
      </div>

      {error && (
        <div role="alert" className="flex items-center justify-between gap-3 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200">
          <span>{error}</span>
          <Button size="sm" variant="outline" onClick={refrescar}>Reintentar</Button>
        </div>
      )}

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative max-w-sm flex-1">
          <label htmlFor="busqueda" className="sr-only">Buscar ticket</label>
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            id="busqueda"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por código, asunto o solicitante"
            className="input py-2 pl-10"
          />
        </div>
        <div className="flex flex-wrap gap-1 rounded-lg border border-slate-200 bg-white p-1" role="group" aria-label="Filtrar por estado">
          {estadosPresentes.map((estado) => (
            <button
              key={estado}
              type="button"
              aria-pressed={filtroVigente === estado}
              onClick={() => setFiltroEstado(estado)}
              className={`rounded-md px-3 py-1.5 text-xs font-medium transition-colors ${
                filtroVigente === estado ? 'bg-sky-600 text-white' : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              {estado}
            </button>
          ))}
        </div>
      </div>

      <Card className="overflow-hidden">
        {cargando ? (
          <Spinner label="Cargando bandeja..." />
        ) : filtrados.length === 0 ? (
          <EmptyState
            icon={<Inbox size={22} />}
            title={tickets.length === 0 ? 'No tiene tickets asignados' : 'Ningún ticket coincide con el filtro'}
            desc={
              tickets.length === 0
                ? 'Los tickets nuevos se asignan automáticamente al técnico con menos carga.'
                : 'Pruebe con otra búsqueda o seleccione "Todos".'
            }
          />
        ) : (
          <ul className="divide-y divide-slate-100">
            {filtrados.map((t) => (
              <li key={t.id}>
                <button
                  type="button"
                  onClick={() => setAbierto(t.id)}
                  className="flex w-full items-center gap-4 px-5 py-4 text-left transition-colors hover:bg-slate-50 focus-visible:bg-slate-50 focus-visible:outline focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-sky-600"
                >
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="font-mono text-xs font-medium text-slate-500">{t.codigo}</span>
                      <EstadoBadge estado={t.estado} />
                      <PrioridadBadge prioridad={t.prioridad} />
                    </div>
                    <p className="mt-1 truncate text-sm font-medium text-slate-900">{t.asunto}</p>
                    <p className="mt-0.5 text-xs text-slate-500">{t.solicitante}</p>
                  </div>
                  <div className="hidden w-44 shrink-0 sm:block">
                    <SlaBar ticket={t} />
                  </div>
                </button>
              </li>
            ))}
          </ul>
        )}
      </Card>

      {abierto && (
        <TicketDrawer key={abierto} ticketId={abierto} onClose={() => setAbierto(null)} onChanged={refrescar} />
      )}
    </div>
  );
}
