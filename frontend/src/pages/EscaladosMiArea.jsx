import { useEffect, useState } from 'react';
import { Building2, Clock, Inbox, RefreshCw } from 'lucide-react';
import { obtenerEscaladosMiArea, obtenerMisAreas } from '../api/areaApi';
import { mensajeDeError } from '../api/ticketsApi';
import TicketDrawer from '../components/TicketDrawer';
import { Button, Card, EmptyState, PrioridadBadge, Spinner, StatCard } from '../components/ui';
import { timeAgo } from '../lib/ui';

const REFRESCO_MS = 60_000;

export default function EscaladosMiArea() {
  const [resultado, setResultado] = useState({ clave: null, areas: [], tickets: [], error: '' });
  const [recarga, setRecarga] = useState(0);
  const [abierto, setAbierto] = useState(null);

  const clave = String(recarga);

  useEffect(() => {
    let activo = true;
    Promise.all([obtenerMisAreas(), obtenerEscaladosMiArea()])
      .then(([areas, tickets]) => activo && setResultado({ clave, areas, tickets, error: '' }))
      .catch((e) =>
        activo && setResultado({ clave, areas: [], tickets: [], error: mensajeDeError(e, 'No se pudieron cargar los tickets de su área.') })
      );
    return () => {
      activo = false;
    };
  }, [clave]);

  useEffect(() => {
    const id = setInterval(() => setRecarga((n) => n + 1), REFRESCO_MS);
    return () => clearInterval(id);
  }, []);

  const cargando = resultado.clave !== clave;
  const { areas, tickets, error } = resultado;
  const variasAreas = areas.length > 1;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Escalados a mi área</h1>
          <p className="mt-1 text-sm text-slate-500">
            Tickets que el equipo de TI escaló a {areas.length === 1 ? 'el área' : 'las áreas'} que usted representa.
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => setRecarga((n) => n + 1)} disabled={cargando}>
          <RefreshCw size={14} className={cargando ? 'animate-spin' : ''} />
          {cargando ? 'Actualizando...' : 'Actualizar'}
        </Button>
      </div>

      {error && (
        <div role="alert" className="flex items-center justify-between gap-3 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200">
          <span>{error}</span>
          <Button size="sm" variant="outline" onClick={() => setRecarga((n) => n + 1)}>Reintentar</Button>
        </div>
      )}

      {areas.length > 0 && (
        <div className="grid grid-cols-2 gap-4">
          <StatCard label="Pendientes de respuesta" value={tickets.length} icon={<Inbox size={18} />} accent={tickets.length > 0 ? 'amber' : 'slate'} />
          <StatCard label={areas.length === 1 ? 'Área que representa' : 'Áreas que representa'}
            value={areas.length === 1 ? areas[0].nombre : areas.length} icon={<Building2 size={18} />} accent="sky" />
        </div>
      )}

      <Card className="overflow-hidden">
        {cargando && tickets.length === 0 && areas.length === 0 ? (
          <Spinner label="Cargando..." />
        ) : areas.length === 0 && !error ? (
          <EmptyState icon={<Building2 size={22} />} title="No es representante de ninguna área"
            desc="Cuando un administrador lo designe representante de un área, aquí verá los tickets que se escalen a ella." />
        ) : tickets.length === 0 && !error ? (
          <EmptyState icon={<Inbox size={22} />} title="No hay tickets escalados a su área"
            desc="Cuando el equipo de TI escale un ticket, recibirá un correo y aparecerá aquí." />
        ) : (
          <ul className="divide-y divide-slate-100">
            {tickets.map((t) => (
              <li key={t.id}>
                <button type="button" onClick={() => setAbierto(t.id)}
                  className="block w-full px-5 py-4 text-left transition-colors hover:bg-slate-50 focus-visible:bg-slate-50 focus-visible:outline focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-sky-600">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-mono text-xs font-medium text-slate-500">{t.codigo}</span>
                    <PrioridadBadge prioridad={t.prioridad} />
                    {variasAreas && <span className="rounded bg-violet-50 px-2 py-0.5 text-xs font-medium text-violet-700">{t.area}</span>}
                    <span className="ml-auto flex items-center gap-1 text-xs text-slate-500">
                      <Clock size={12} /> escalado {timeAgo(t.fechaEscalamiento)}
                    </span>
                  </div>
                  <p className="mt-1 truncate text-sm font-medium text-slate-900">{t.asunto}</p>
                  <p className="mt-0.5 line-clamp-2 text-xs text-slate-600">Motivo: {t.motivo}</p>
                  <p className="mt-1 text-xs text-slate-500">
                    Solicitante: {t.solicitante} · Técnico: {t.tecnico ?? 'sin asignar'}
                  </p>
                </button>
              </li>
            ))}
          </ul>
        )}
      </Card>

      {abierto && (
        <TicketDrawer key={abierto} ticketId={abierto} onClose={() => setAbierto(null)}
          onChanged={() => setRecarga((n) => n + 1)} puedeActuar={false} permitirRespuestaArea />
      )}
    </div>
  );
}
