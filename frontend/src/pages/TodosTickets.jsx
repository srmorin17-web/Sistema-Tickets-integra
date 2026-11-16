import { useEffect, useMemo, useState } from 'react';
import { CheckCircle2, RefreshCw, Search, Ticket } from 'lucide-react';
import { obtenerTodosLosTickets } from '../api/adminApi';
import { mensajeDeError } from '../api/ticketsApi';
import TicketDrawer from '../components/TicketDrawer';
import { Button, Card, EmptyState, EstadoBadge, PrioridadBadge, Spinner, StatCard } from '../components/ui';
import { formatDate } from '../lib/ui';

export default function TodosTickets() {
  const [tickets, setTickets] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [actualizando, setActualizando] = useState(false);
  const [error, setError] = useState('');
  const [recarga, setRecarga] = useState(0);
  const [busqueda, setBusqueda] = useState('');
  const [filtroEstado, setFiltroEstado] = useState('Todos');
  const [detalle, setDetalle] = useState(null);

  useEffect(() => {
    let activo = true;
    obtenerTodosLosTickets()
      .then((datos) => {
        if (!activo) return;
        setTickets(datos);
        setError('');
      })
      .catch((e) => {
        if (activo) setError(mensajeDeError(e, 'No se pudo cargar el historial de tickets.'));
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

  const estados = useMemo(() => ['Todos', ...new Set(tickets.map((t) => t.estado))], [tickets]);
  const filtrados = useMemo(() => {
    const q = busqueda.trim().toLowerCase();
    return tickets.filter((t) =>
      (filtroEstado === 'Todos' || t.estado === filtroEstado) &&
      (!q || [t.codigo, t.asunto, t.solicitante, t.correoSolicitante, t.tecnicoAsignado, t.categoria]
        .some((valor) => valor?.toLowerCase().includes(q)))
    );
  }, [tickets, busqueda, filtroEstado]);

  const actualizar = () => {
    setActualizando(true);
    setRecarga((n) => n + 1);
  };

  const finalizados = tickets.filter((t) => ['Resuelto', 'Cerrado', 'Cancelado'].includes(t.estado)).length;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Todos los tickets</h1>
          <p className="mt-1 text-sm text-slate-500">Consulta tickets activos y finalizados, y abre su historial completo.</p>
        </div>
        <Button variant="outline" size="sm" onClick={actualizar} disabled={actualizando}>
          <RefreshCw size={14} className={actualizando ? 'animate-spin' : ''} />
          {actualizando ? 'Actualizando...' : 'Actualizar'}
        </Button>
      </div>

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-3">
        <StatCard label="Total de tickets" value={tickets.length} icon={<Ticket size={18} />} accent="sky" />
        <StatCard label="Activos" value={tickets.length - finalizados} icon={<RefreshCw size={18} />} accent="amber" />
        <StatCard label="Finalizados" value={finalizados} icon={<CheckCircle2 size={18} />} accent="emerald" />
      </div>

      {error && (
        <div role="alert" className="flex items-center justify-between gap-3 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200">
          <span>{error}</span>
          <Button size="sm" variant="outline" onClick={actualizar}>Reintentar</Button>
        </div>
      )}

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative max-w-sm flex-1">
          <label htmlFor="busqueda-todos-tickets" className="sr-only">Buscar ticket</label>
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            id="busqueda-todos-tickets"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por código, asunto, solicitante o técnico"
            className="input py-2 pl-10"
          />
        </div>
        <label className="flex items-center gap-2 text-sm text-slate-600">
          Estado
          <select value={filtroEstado} onChange={(e) => setFiltroEstado(e.target.value)} className="input py-2">
            {estados.map((estado) => <option key={estado}>{estado}</option>)}
          </select>
        </label>
      </div>

      <Card className="overflow-hidden">
        {cargando && tickets.length === 0 ? (
          <Spinner label="Cargando todos los tickets..." />
        ) : filtrados.length === 0 ? (
          <EmptyState
            icon={<Ticket size={22} />}
            title={tickets.length === 0 ? 'No hay tickets registrados' : 'Ningún ticket coincide con los filtros'}
            desc={tickets.length === 0 ? 'Los tickets del sistema aparecerán aquí.' : 'Pruebe otra búsqueda o seleccione otro estado.'}
          />
        ) : (
          <ul className="divide-y divide-slate-100">
            {filtrados.map((t) => (
              <li key={t.id}>
                <button
                  type="button"
                  onClick={() => setDetalle(t.id)}
                  className="w-full px-5 py-4 text-left transition-colors hover:bg-slate-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-inset focus-visible:outline-sky-600"
                >
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-mono text-xs font-medium text-slate-500">{t.codigo}</span>
                    <EstadoBadge estado={t.estado} />
                    <PrioridadBadge prioridad={t.prioridad} />
                    <span className="ml-auto text-xs text-slate-500">{formatDate(t.fechaCreacion)}</span>
                  </div>
                  <p className="mt-1 truncate text-sm font-medium text-slate-900">{t.asunto}</p>
                  <p className="mt-0.5 text-xs text-slate-500">
                    {t.solicitante} · {t.tecnicoAsignado ? `atiende ${t.tecnicoAsignado}` : 'sin técnico asignado'} · {t.categoria}
                  </p>
                </button>
              </li>
            ))}
          </ul>
        )}
      </Card>

      {detalle && (
        <TicketDrawer
          key={detalle}
          ticketId={detalle}
          onClose={() => setDetalle(null)}
          puedeActuar={false}
          soloLectura
        />
      )}
    </div>
  );
}
