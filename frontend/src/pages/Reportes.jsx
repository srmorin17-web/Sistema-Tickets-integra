import { useEffect, useState } from 'react';
import { Download, FileSpreadsheet, FileText, RefreshCw } from 'lucide-react';
import { mensajeDeError } from '../api/ticketsApi';
import { obtenerTecnicos } from '../api/asignacionApi';
import { descargarReporte, obtenerReporte } from '../api/reportesApi';
import { useCatalogos } from '../context/CatalogosContext';
import { Button, Card, EmptyState, EstadoBadge, PrioridadBadge, Spinner } from '../components/ui';
import { finDelDia, inicioDelDia, rangoUltimosDias } from '../lib/fechas';
import { formatDate } from '../lib/ui';

const FORMATOS = [
  { id: 'excel', etiqueta: 'Excel (.xlsx)', icono: FileSpreadsheet, ayuda: 'Hoja de cálculo para filtrar y analizar.' },
  { id: 'pdf', etiqueta: 'PDF', icono: FileText, ayuda: 'Documento listo para imprimir o archivar.' },
];

export default function Reportes() {
  const { catalogos } = useCatalogos();
  const [rango, setRango] = useState(() => rangoUltimosDias(30));
  const [categoriaId, setCategoriaId] = useState('');
  const [tecnicoId, setTecnicoId] = useState('');
  const [tecnicos, setTecnicos] = useState([]);
  const [generando, setGenerando] = useState(null); // 'excel' | 'pdf' | null
  const [resultado, setResultado] = useState({ clave: null, filas: [], error: '' });
  const [recarga, setRecarga] = useState(0);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');

  // Si no se puede cargar el listado, el filtro queda solo con "Todos"
  useEffect(() => {
    let activo = true;
    obtenerTecnicos()
      .then((lista) => activo && setTecnicos(lista))
      .catch(() => {});
    return () => {
      activo = false;
    };
  }, []);

  const rangoValido = Boolean(rango.desde && rango.hasta && rango.desde <= rango.hasta);
  const clave = rangoValido
    ? JSON.stringify([rango.desde, rango.hasta, categoriaId, tecnicoId, recarga])
    : null;

  useEffect(() => {
    if (!clave) return undefined;

    let activo = true;
    obtenerReporte({
      inicio: inicioDelDia(rango.desde),
      fin: finDelDia(rango.hasta),
      categoriaId,
      tecnicoId,
    })
      .then((datos) => {
        if (activo) setResultado({ clave, filas: datos, error: '' });
      })
      .catch((e) => {
        if (activo) {
          setResultado({
            clave,
            filas: [],
            error: mensajeDeError(e, 'No se pudo cargar la tabla del reporte.'),
          });
        }
      });
    return () => {
      activo = false;
    };
  }, [clave, rango.desde, rango.hasta, categoriaId, tecnicoId]);

  const cargandoFilas = Boolean(clave && resultado.clave !== clave);
  const filas = resultado.clave === clave ? resultado.filas : [];
  const errorFilas = resultado.clave === clave ? resultado.error : '';

  const generar = async (formato) => {
    setGenerando(formato);
    setError('');
    setAviso('');
    try {
      await descargarReporte(formato, {
        inicio: inicioDelDia(rango.desde),
        fin: finDelDia(rango.hasta),
        categoriaId,
        tecnicoId,
      });
      setAviso('El reporte se generó y se descargó correctamente.');
    } catch (e) {
      setError(mensajeDeError(e, 'No se pudo generar el reporte. Intente nuevamente.'));
    } finally {
      setGenerando(null);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Reportes</h1>
        <p className="mt-1 text-sm text-slate-500">
          Listado de tickets creados en el período, con técnico, estado, tiempo de resolución y cumplimiento de SLA.
        </p>
      </div>

      <Card className="space-y-5 p-5">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <label htmlFor="rep-desde" className="mb-1.5 block text-sm font-medium text-slate-700">Desde</label>
            <input id="rep-desde" type="date" value={rango.desde} max={rango.hasta || undefined}
              onChange={(e) => setRango((r) => ({ ...r, desde: e.target.value }))} className="input" />
          </div>
          <div>
            <label htmlFor="rep-hasta" className="mb-1.5 block text-sm font-medium text-slate-700">Hasta</label>
            <input id="rep-hasta" type="date" value={rango.hasta} min={rango.desde || undefined}
              onChange={(e) => setRango((r) => ({ ...r, hasta: e.target.value }))} className="input" />
          </div>
          <div>
            <label htmlFor="rep-categoria" className="mb-1.5 block text-sm font-medium text-slate-700">Categoría</label>
            <select id="rep-categoria" value={categoriaId} onChange={(e) => setCategoriaId(e.target.value)} className="input">
              <option value="">Todas</option>
              {(catalogos?.categorias ?? []).map((c) => (
                <option key={c.id} value={c.id}>{c.nombre}</option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="rep-tecnico" className="mb-1.5 block text-sm font-medium text-slate-700">Técnico</label>
            <select id="rep-tecnico" value={tecnicoId} onChange={(e) => setTecnicoId(e.target.value)} className="input">
              <option value="">Todos</option>
              {tecnicos.map((t) => (
                <option key={t.id} value={t.id}>{t.nombre}</option>
              ))}
            </select>
          </div>
        </div>

        {!rangoValido && (
          <p role="alert" className="text-sm text-amber-700">
            La fecha inicial no puede ser posterior a la final.
          </p>
        )}

        <div>
          <h2 className="mb-3 text-sm font-semibold text-slate-900">Descargar reporte</h2>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {FORMATOS.map(({ id, etiqueta, icono: Icono, ayuda }) => (
            <div key={id} className="flex items-center justify-between gap-3 rounded-xl border border-slate-200 p-4">
              <div className="flex min-w-0 items-center gap-3">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-sky-50 text-sky-600">
                  <Icono size={20} />
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-medium text-slate-900">{etiqueta}</p>
                  <p className="text-xs text-slate-500">{ayuda}</p>
                </div>
              </div>
              <Button size="sm" disabled={!rangoValido || generando !== null} onClick={() => generar(id)}>
                <Download size={14} />
                {generando === id ? 'Generando...' : 'Descargar'}
              </Button>
            </div>
          ))}
          </div>
        </div>

        {aviso && (
          <p role="status" className="rounded-lg bg-emerald-50 px-3 py-2.5 text-sm text-emerald-700 ring-1 ring-emerald-200">{aviso}</p>
        )}
        {error && (
          <p role="alert" className="rounded-lg bg-rose-50 px-3 py-2.5 text-sm text-rose-700 ring-1 ring-rose-200">{error}</p>
        )}
      </Card>

      <section className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">Detalle de tickets</h2>
            <p className="text-sm text-slate-500">{rangoValido ? filas.length : 0} {filas.length === 1 ? 'resultado' : 'resultados'} para los filtros seleccionados.</p>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setRecarga((n) => n + 1)}
            disabled={!rangoValido || cargandoFilas}
          >
            <RefreshCw size={14} className={cargandoFilas ? 'animate-spin' : ''} />
            {cargandoFilas ? 'Actualizando...' : 'Actualizar tabla'}
          </Button>
        </div>

        {errorFilas && (
          <div role="alert" className="flex items-center justify-between gap-3 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200">
            <span>{errorFilas}</span>
            <Button size="sm" variant="outline" onClick={() => setRecarga((n) => n + 1)}>Reintentar</Button>
          </div>
        )}

        <Card className="overflow-hidden">
          {!rangoValido ? (
            <EmptyState title="Revise el período" desc="La fecha inicial no puede ser posterior a la final." />
          ) : cargandoFilas && filas.length === 0 ? (
            <Spinner label="Cargando detalle del reporte..." />
          ) : errorFilas ? (
            <EmptyState title="No se pudo cargar la tabla" desc="Revise el error anterior o vuelva a intentarlo." />
          ) : filas.length === 0 ? (
            <EmptyState title="No hay tickets para mostrar" desc="Pruebe con otro período o cambie los filtros." />
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-[1100px] w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs font-medium uppercase tracking-wide text-slate-500">
                  <tr>
                    <th scope="col" className="px-4 py-3">Código</th>
                    <th scope="col" className="px-4 py-3">Asunto</th>
                    <th scope="col" className="px-4 py-3">Solicitante</th>
                    <th scope="col" className="px-4 py-3">Técnico</th>
                    <th scope="col" className="px-4 py-3">Categoría</th>
                    <th scope="col" className="px-4 py-3">Prioridad</th>
                    <th scope="col" className="px-4 py-3">Estado</th>
                    <th scope="col" className="px-4 py-3">Creación</th>
                    <th scope="col" className="px-4 py-3">Cierre</th>
                    <th scope="col" className="px-4 py-3">Resolución</th>
                    <th scope="col" className="px-4 py-3">SLA cumplido</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filas.map((fila) => (
                    <tr key={fila.codigo} className="text-slate-700 hover:bg-slate-50">
                      <td className="whitespace-nowrap px-4 py-3 font-mono text-xs">{fila.codigo}</td>
                      <td className="max-w-56 truncate px-4 py-3" title={fila.asunto}>{fila.asunto}</td>
                      <td className="whitespace-nowrap px-4 py-3">{fila.solicitante}</td>
                      <td className="whitespace-nowrap px-4 py-3">{fila.tecnico}</td>
                      <td className="whitespace-nowrap px-4 py-3">{fila.categoria}</td>
                      <td className="whitespace-nowrap px-4 py-3"><PrioridadBadge prioridad={fila.prioridad} /></td>
                      <td className="whitespace-nowrap px-4 py-3"><EstadoBadge estado={fila.estado} /></td>
                      <td className="whitespace-nowrap px-4 py-3">{formatDate(fila.fechaCreacion)}</td>
                      <td className="whitespace-nowrap px-4 py-3">{fila.fechaCierre === 'En curso' ? fila.fechaCierre : formatDate(fila.fechaCierre)}</td>
                      <td className="whitespace-nowrap px-4 py-3">{fila.tiempoResolucion}</td>
                      <td className="whitespace-nowrap px-4 py-3">{fila.slaCumplido}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </section>
    </div>
  );
}
