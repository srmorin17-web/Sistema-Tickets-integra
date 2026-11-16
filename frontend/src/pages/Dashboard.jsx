import { useEffect, useState } from 'react';
import { AlertTriangle, CheckCircle2, Inbox, RefreshCw, Send, Timer } from 'lucide-react';
import { mensajeDeError } from '../api/ticketsApi';
import { obtenerDashboard } from '../api/metricasApi';
import { obtenerEstadoSincronizacionRrhh, sincronizarRrhhAhora } from '../api/rrhhSyncApi';
import { GraficoBarras, GraficoDona } from '../components/graficos';
import { Button, Card, Spinner, StatCard } from '../components/ui';
import { useAuth } from '../context/AuthContext';
import { COLOR_ESTADO, COLOR_PRIORIDAD } from '../lib/colores';
import { finDelDia, inicioDelDia, rangoUltimosDias } from '../lib/fechas';

const PERIODOS = [
  { id: '7', etiqueta: '7 días', dias: 7 },
  { id: '30', etiqueta: '30 días', dias: 30 },
  { id: '90', etiqueta: '90 días', dias: 90 },
  { id: 'personalizado', etiqueta: 'Personalizado' },
];

function Panel({ titulo, nota, children, className = '' }) {
  return (
    <Card className={`p-5 ${className}`}>
      <h2 className="text-sm font-semibold text-slate-900">{titulo}</h2>
      {nota && <p className="mt-0.5 text-xs text-slate-500">{nota}</p>}
      <div className="mt-3">{children}</div>
    </Card>
  );
}

export default function Dashboard() {
  const { user } = useAuth();
  const [periodo, setPeriodo] = useState('30');
  const [rango, setRango] = useState(() => rangoUltimosDias(30));
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, datos: null, error: '' });
  const [estadoRrhh, setEstadoRrhh] = useState(null);
  const [errorRrhh, setErrorRrhh] = useState('');
  const [mensajeAccionRrhh, setMensajeAccionRrhh] = useState('');
  const [sincronizandoRrhh, setSincronizandoRrhh] = useState(false);
  const [recargaRrhh, setRecargaRrhh] = useState(0);
  const [ahora, setAhora] = useState(0);
  const puedeVerEstadoRrhh = ['Administrador', 'Superadmin'].includes(user?.rol);

  const rangoValido = Boolean(rango.desde && rango.hasta && rango.desde <= rango.hasta);
  const clave = `${rango.desde}|${rango.hasta}|${recarga}`;

  useEffect(() => {
    if (!rangoValido) return undefined;
    let activo = true;
    obtenerDashboard({ inicio: inicioDelDia(rango.desde), fin: finDelDia(rango.hasta) })
      .then((datos) => activo && setResultado({ clave, datos, error: '' }))
      .catch((e) =>
        activo && setResultado({ clave, datos: null, error: mensajeDeError(e, 'No se pudieron cargar las métricas.') })
      );
    return () => {
      activo = false;
    };
  }, [clave, rangoValido, rango.desde, rango.hasta]);

  useEffect(() => {
    if (!puedeVerEstadoRrhh) return undefined;
    let activo = true;
    const cargarEstado = async () => {
      try {
        const estado = await obtenerEstadoSincronizacionRrhh();
        if (activo) {
          setEstadoRrhh(estado);
          setAhora(Date.now());
          setErrorRrhh('');
        }
      } catch {
        if (activo) setErrorRrhh('No se pudo consultar el estado de RR. HH.');
      }
    };
    cargarEstado();
    const temporizador = window.setInterval(cargarEstado, 60_000);
    return () => {
      activo = false;
      window.clearInterval(temporizador);
    };
  }, [puedeVerEstadoRrhh, recargaRrhh]);

  const cooldownRrhh = estadoRrhh?.ultimoIntentoManual
    ? Math.max(0, Math.ceil(
      (new Date(estadoRrhh.ultimoIntentoManual).getTime() + 120_000 - ahora) / 1000
    ))
    : 0;
  const tieneCooldownRrhh = cooldownRrhh > 0;
  const ultimoIntentoManual = estadoRrhh?.ultimoIntentoManual;

  useEffect(() => {
    if (!ultimoIntentoManual || !tieneCooldownRrhh) return undefined;
    const temporizador = window.setInterval(() => setAhora(Date.now()), 1000);
    return () => window.clearInterval(temporizador);
  }, [ultimoIntentoManual, tieneCooldownRrhh]);

  const cargando = rangoValido && resultado.clave !== clave;
  const { datos, error } = resultado;

  const elegirPeriodo = (p) => {
    setPeriodo(p.id);
    if (p.dias) setRango(rangoUltimosDias(p.dias));
  };

  const fechaRrhh = (valor) => (valor ? new Date(valor).toLocaleString() : 'Aún no disponible');
  const tiempoCooldownRrhh = `${Math.floor(cooldownRrhh / 60)}:${String(cooldownRrhh % 60).padStart(2, '0')}`;

  const sincronizarRrhh = async () => {
    setSincronizandoRrhh(true);
    setMensajeAccionRrhh('');
    try {
      const estado = await sincronizarRrhhAhora();
      setEstadoRrhh(estado);
      setAhora(Date.now());
      setErrorRrhh('');
      setMensajeAccionRrhh('Sincronización con RR. HH. completada.');
    } catch (e) {
      setMensajeAccionRrhh(mensajeDeError(e, e.response?.data?.detail || 'No se pudo sincronizar con RR. HH.'));
      setRecargaRrhh((n) => n + 1);
    } finally {
      setSincronizandoRrhh(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Dashboard</h1>
          <p className="mt-1 text-sm text-slate-500">Indicadores de tickets creados en el período seleccionado.</p>
        </div>
        <Button variant="outline" size="sm" onClick={() => setRecarga((n) => n + 1)} disabled={cargando}>
          <RefreshCw size={14} className={cargando ? 'animate-spin' : ''} />
          {cargando ? 'Actualizando...' : 'Actualizar'}
        </Button>
      </div>

      {puedeVerEstadoRrhh && (
        <Card className="flex flex-col gap-3 p-5 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-sm font-semibold text-slate-900">Integración con RR. HH.</h2>
            {errorRrhh ? (
              <p role="alert" className="mt-1 text-sm text-rose-700">{errorRrhh}</p>
            ) : estadoRrhh ? (
              <>
                <p className="mt-1 text-sm text-slate-700">
                  Estado:{' '}
                  <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
                    estadoRrhh.estado === 'CONECTADA'
                      ? 'bg-emerald-100 text-emerald-800'
                      : estadoRrhh.estado === 'ERROR'
                        ? 'bg-rose-100 text-rose-800'
                        : 'bg-amber-100 text-amber-800'
                  }`}>
                    {estadoRrhh.estado}
                  </span>
                  {estadoRrhh.habilitada && ` · sincronización cada ${estadoRrhh.intervaloMinutos} min`}
                </p>
                <p className="mt-1 text-xs text-slate-500">
                  Última conexión: {fechaRrhh(estadoRrhh.ultimaConexion)}
                  {' · '}Última sincronización: {fechaRrhh(estadoRrhh.ultimaSincronizacion)}
                </p>
                {estadoRrhh.ultimoError && (
                  <p role="alert" className="mt-1 text-xs text-rose-700">{estadoRrhh.ultimoError}</p>
                )}
              </>
            ) : (
              <p className="mt-1 text-sm text-slate-500">Consultando estado de conexión...</p>
            )}
          </div>
          <div className="flex flex-col items-start gap-2 sm:items-end">
            <div className="flex flex-wrap gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setRecargaRrhh((n) => n + 1)}
              >
                <RefreshCw size={14} /> Actualizar estado
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={sincronizarRrhh}
                disabled={!estadoRrhh?.habilitada || sincronizandoRrhh || estadoRrhh?.estado === 'CONECTANDO' || cooldownRrhh > 0}
              >
                <RefreshCw size={14} className={sincronizandoRrhh ? 'animate-spin' : ''} />
                {sincronizandoRrhh ? 'Sincronizando...' : cooldownRrhh > 0 ? `Disponible en ${tiempoCooldownRrhh}` : 'Sincronizar ahora'}
              </Button>
            </div>
            {mensajeAccionRrhh && (
              <p role="status" className={`text-xs ${mensajeAccionRrhh.includes('completada') ? 'text-emerald-700' : 'text-rose-700'}`}>
                {mensajeAccionRrhh}
              </p>
            )}
          </div>
        </Card>
      )}

      <div className="flex flex-wrap items-end gap-3">
        <div className="flex gap-1 rounded-lg border border-slate-200 bg-white p-1" role="group" aria-label="Período">
          {PERIODOS.map((p) => (
            <button
              key={p.id}
              type="button"
              aria-pressed={periodo === p.id}
              onClick={() => elegirPeriodo(p)}
              className={`rounded-md px-3 py-1.5 text-xs font-medium transition-colors ${
                periodo === p.id ? 'bg-sky-600 text-white' : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              {p.etiqueta}
            </button>
          ))}
        </div>
        {periodo === 'personalizado' && (
          <div className="flex items-end gap-2">
            <div>
              <label htmlFor="desde" className="mb-1 block text-xs font-medium text-slate-600">Desde</label>
              <input id="desde" type="date" value={rango.desde} max={rango.hasta || undefined}
                onChange={(e) => setRango((r) => ({ ...r, desde: e.target.value }))} className="input py-1.5" />
            </div>
            <div>
              <label htmlFor="hasta" className="mb-1 block text-xs font-medium text-slate-600">Hasta</label>
              <input id="hasta" type="date" value={rango.hasta} min={rango.desde || undefined}
                onChange={(e) => setRango((r) => ({ ...r, hasta: e.target.value }))} className="input py-1.5" />
            </div>
          </div>
        )}
      </div>

      {!rangoValido && (
        <div role="alert" className="rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-800 ring-1 ring-amber-200">
          Seleccione un rango de fechas válido: la fecha inicial no puede ser posterior a la final.
        </div>
      )}

      {error && (
        <div role="alert" className="flex items-center justify-between gap-3 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200">
          <span>{error}</span>
          <Button size="sm" variant="outline" onClick={() => setRecarga((n) => n + 1)}>Reintentar</Button>
        </div>
      )}

      {!datos && cargando && <Spinner label="Cargando indicadores..." />}

      {datos && (
        <div className={`space-y-6 transition-opacity ${cargando ? 'opacity-60' : ''}`} aria-busy={cargando}>
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            <StatCard label="Tickets abiertos" value={datos.ticketsAbiertos} icon={<Inbox size={18} />} accent="sky" />
            <StatCard label="Resueltos" value={datos.ticketsResueltos} icon={<CheckCircle2 size={18} />} accent="emerald"
              sub="Incluye cerrados" />
            <StatCard label="Escalados" value={datos.ticketsEscalados} icon={<Send size={18} />} accent="violet" />
            <StatCard label="SLA cumplido" value={`${datos.porcentajeSlaCumplido}%`} icon={<Timer size={18} />} accent="amber"
              sub="Sobre tickets finalizados" />
          </div>

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
            <Panel titulo="Distribución por estado" className="lg:col-span-2">
              <GraficoBarras titulo="Tickets por estado" datos={datos.ticketsPorEstado} colorDe={COLOR_ESTADO} />
            </Panel>
            <Panel titulo="Por categoría">
              <GraficoDona titulo="Tickets por categoría" datos={datos.ticketsPorCategoria} />
            </Panel>
          </div>

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <Panel titulo="Por prioridad">
              <GraficoBarras titulo="Tickets por prioridad" datos={datos.ticketsPorPrioridad} colorDe={COLOR_PRIORIDAD} />
            </Panel>
            <Panel titulo="Carga por técnico" nota="Tickets activos asignados en este momento; no depende del período.">
              <GraficoBarras titulo="Carga actual por técnico" datos={datos.cargaPorTecnico} horizontal />
            </Panel>
          </div>

          {datos.ticketsPorEstado.length === 0 && (
            <p className="flex items-center gap-2 text-sm text-slate-500">
              <AlertTriangle size={15} /> No se registraron tickets en el período seleccionado.
            </p>
          )}
        </div>
      )}
    </div>
  );
}
