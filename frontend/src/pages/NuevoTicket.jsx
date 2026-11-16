import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { CheckCircle2, Info, Paperclip, Send, X } from 'lucide-react';
import { subirAdjunto } from '../api/adjuntosApi';
import { crearTicket } from '../api/ticketsApi';
import { useCatalogos } from '../context/CatalogosContext';
import { Button, Card, PrioridadBadge, Spinner } from '../components/ui';
import { ACCEPT, MAX_ADJUNTOS, formatBytes, mensajeDeAdjunto, validarArchivo } from '../lib/adjuntos';

const MAX_ASUNTO = 200;
const MIN_DESCRIPCION = 10;
const MAX_DESCRIPCION = 4000;

function mensajeDeCreacion(error) {
  if (!error.response) return 'No hay conexión con el servidor.';
  switch (error.response.status) {
    case 400:
      return 'Revise los datos del formulario e intente nuevamente.';
    case 422:
      return 'La categoría seleccionada aún no tiene una prioridad configurada. Comuníquese con el administrador del sistema.';
    case 403:
      return 'No se pudo validar que usted figure como empleado activo en RR. HH. Comuníquese con el área de TI.';
    case 409:
      return 'Ya tiene un ticket activo. Espere a que sea resuelto o cerrado antes de crear otro.';
    default:
      return 'No se pudo registrar el ticket. Intente nuevamente; si el problema persiste, comuníquese con el área de TI.';
  }
}

export default function NuevoTicket() {
  const { catalogos, error: errorCatalogos, recargar } = useCatalogos();
  const [asunto, setAsunto] = useState('');
  const [categoriaId, setCategoriaId] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');
  const [creado, setCreado] = useState(null); // { codigo, prioridad, adjuntados, fallidos }
  const [archivos, setArchivos] = useState([]);
  const [erroresArchivos, setErroresArchivos] = useState([]);
  const [progreso, setProgreso] = useState('');
  const entradaArchivos = useRef(null);

  const categoria = catalogos?.categorias.find((c) => c.id === categoriaId);
  const valido = asunto.trim() && categoriaId && descripcion.trim().length >= MIN_DESCRIPCION;

  const elegirArchivos = (e) => {
    const elegidos = Array.from(e.target.files ?? []);
    e.target.value = ''; // permite elegir de nuevo el mismo archivo
    const errores = [];
    const aceptados = [...archivos];
    for (const archivo of elegidos) {
      if (aceptados.some((a) => a.name === archivo.name && a.size === archivo.size)) continue;
      const motivo = validarArchivo(archivo, aceptados.length);
      if (motivo) errores.push(motivo);
      else aceptados.push(archivo);
    }
    setArchivos(aceptados);
    setErroresArchivos(errores);
  };

  const enviar = async (e) => {
    e.preventDefault();
    if (!valido || enviando) return;
    setEnviando(true);
    setError('');
    try {
      const respuesta = await crearTicket({
        asunto: asunto.trim(),
        descripcion: descripcion.trim(),
        categoriaId,
      });

      // El ticket ya existe: los archivos se suben uno por uno y un fallo no deshace el registro
      const fallidos = [];
      let adjuntados = 0;
      for (const [indice, archivo] of archivos.entries()) {
        setProgreso(`Subiendo archivo ${indice + 1} de ${archivos.length}...`);
        try {
          await subirAdjunto(respuesta.ticketId, archivo);
          adjuntados += 1;
        } catch (errArchivo) {
          fallidos.push(`${archivo.name}: ${mensajeDeAdjunto(errArchivo)}`);
        }
      }
      setCreado({ codigo: respuesta.codigo, prioridad: respuesta.prioridad ?? categoria?.prioridad, adjuntados, fallidos });
    } catch (err) {
      setError(mensajeDeCreacion(err));
    } finally {
      setEnviando(false);
      setProgreso('');
    }
  };

  const registrarOtro = () => {
    setAsunto('');
    setCategoriaId('');
    setDescripcion('');
    setError('');
    setArchivos([]);
    setErroresArchivos([]);
    setCreado(null);
  };

  if (creado) {
    return (
      <div className="mx-auto max-w-xl space-y-6">
        <Card className="p-8 text-center">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-emerald-50 text-emerald-600">
            <CheckCircle2 size={28} />
          </div>
          <h1 className="mt-4 text-xl font-semibold tracking-tight text-slate-900">Ticket registrado</h1>
          <p className="mt-1 text-sm text-slate-500">El equipo de TI ya recibió su solicitud.</p>
          <p className="mt-5 font-mono text-lg font-semibold text-slate-900">{creado.codigo}</p>
          {creado.prioridad && (
            <div className="mt-3 flex items-center justify-center gap-2 text-sm text-slate-600">
              Prioridad asignada <PrioridadBadge prioridad={creado.prioridad} />
            </div>
          )}
          {creado.adjuntados > 0 && (
            <p className="mt-3 text-sm text-slate-600">
              {creado.adjuntados} {creado.adjuntados === 1 ? 'archivo adjuntado' : 'archivos adjuntados'}.
            </p>
          )}
          {creado.fallidos.length > 0 && (
            <div role="alert" className="mx-auto mt-4 max-w-md rounded-lg bg-amber-50 px-4 py-3 text-left text-sm text-amber-800 ring-1 ring-amber-200">
              <p className="font-medium">
                El ticket se registró, pero {creado.fallidos.length === 1 ? 'un archivo no se pudo adjuntar' : `${creado.fallidos.length} archivos no se pudieron adjuntar`}:
              </p>
              <ul className="mt-1 list-disc pl-5">
                {creado.fallidos.map((f) => <li key={f}>{f}</li>)}
              </ul>
              <p className="mt-1 text-xs">Puede volver a adjuntarlos desde el detalle del ticket, en "Mis tickets".</p>
            </div>
          )}
          <div className="mt-6 flex flex-wrap justify-center gap-2">
            <Link
              to="/mis-tickets"
              className="inline-flex items-center rounded-lg bg-sky-600 px-4 py-2 text-sm font-medium text-white shadow-sm transition-colors hover:bg-sky-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-600"
            >
              Ver mis tickets
            </Link>
            <Button variant="outline" onClick={registrarOtro}>Registrar otro ticket</Button>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Nuevo ticket</h1>
        <p className="mt-1 text-sm text-slate-500">Describa su incidencia o solicitud para que el equipo de TI pueda atenderla.</p>
      </div>

      {errorCatalogos && (
        <div role="alert" className="flex items-center justify-between gap-3 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200">
          <span>{errorCatalogos}</span>
          <Button size="sm" variant="outline" onClick={recargar}>Reintentar</Button>
        </div>
      )}

      {!catalogos && !errorCatalogos && <Spinner label="Cargando formulario..." />}

      {catalogos && (
        <Card className="p-6">
          <form onSubmit={enviar} className="space-y-5" noValidate>
            <div>
              <label htmlFor="asunto" className="mb-1.5 block text-sm font-medium text-slate-700">Asunto</label>
              <input id="asunto" value={asunto} onChange={(e) => setAsunto(e.target.value)} maxLength={MAX_ASUNTO}
                placeholder="Resuma el problema en una frase" className="input" required aria-describedby="asunto-ayuda" />
              <p id="asunto-ayuda" className="mt-1 text-right text-xs text-slate-500">{asunto.length}/{MAX_ASUNTO}</p>
            </div>

            <div>
              <label htmlFor="categoria" className="mb-1.5 block text-sm font-medium text-slate-700">Categoría</label>
              <select id="categoria" value={categoriaId} onChange={(e) => setCategoriaId(e.target.value)}
                className="input" required aria-describedby="categoria-ayuda">
                <option value="">Seleccione una categoría...</option>
                {catalogos.categorias.map((c) => (
                  <option key={c.id} value={c.id}>{c.nombre}</option>
                ))}
              </select>
              <div id="categoria-ayuda" className="mt-2 min-h-5 text-xs text-slate-500">
                {categoria && (
                  <div className="flex items-start gap-2 rounded-lg bg-sky-50 px-3 py-2 text-sky-900">
                    <Info size={14} className="mt-0.5 shrink-0" />
                    <span>
                      {categoria.descripcion && <>{categoria.descripcion}. </>}
                      {categoria.prioridad
                        ? <>El sistema le asignará prioridad <strong>{categoria.prioridad}</strong>
                            {categoria.slaHoras ? <> (plazo objetivo de {categoria.slaHoras} h en horario laboral)</> : null}.</>
                        : <>El sistema definirá la prioridad automáticamente.</>}
                    </span>
                  </div>
                )}
              </div>
            </div>

            <div>
              <label htmlFor="descripcion" className="mb-1.5 block text-sm font-medium text-slate-700">Descripción</label>
              <textarea id="descripcion" value={descripcion} onChange={(e) => setDescripcion(e.target.value)}
                rows={6} maxLength={MAX_DESCRIPCION} className="input resize-y" required aria-describedby="descripcion-ayuda"
                placeholder="¿Qué ocurrió? ¿Desde cuándo? Indique el equipo o sistema afectado y los pasos para reproducirlo." />
              <p id="descripcion-ayuda" className="mt-1 text-xs text-slate-500">Mínimo {MIN_DESCRIPCION} caracteres.</p>
            </div>

            <div>
              <div className="mb-1.5 flex items-center justify-between">
                <span className="text-sm font-medium text-slate-700">Adjuntos (opcional)</span>
                <span className="text-xs text-slate-500">{archivos.length} de {MAX_ADJUNTOS}</span>
              </div>
              {archivos.length > 0 && (
                <ul className="mb-2 divide-y divide-slate-100 rounded-lg border border-slate-200">
                  {archivos.map((a) => (
                    <li key={`${a.name}-${a.size}`} className="flex items-center justify-between gap-2 px-3 py-2">
                      <span className="min-w-0 truncate text-sm text-slate-700" title={a.name}>
                        <Paperclip size={13} className="mr-1.5 inline text-slate-400" />{a.name}
                        <span className="ml-2 text-xs text-slate-500">{formatBytes(a.size)}</span>
                      </span>
                      <button type="button" onClick={() => setArchivos(archivos.filter((x) => x !== a))} disabled={enviando}
                        aria-label={`Quitar ${a.name}`} className="rounded p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 disabled:opacity-50">
                        <X size={15} />
                      </button>
                    </li>
                  ))}
                </ul>
              )}
              {archivos.length < MAX_ADJUNTOS && (
                <Button variant="outline" size="sm" onClick={() => entradaArchivos.current?.click()} disabled={enviando}>
                  <Paperclip size={14} /> Agregar archivos
                </Button>
              )}
              <input ref={entradaArchivos} type="file" multiple accept={ACCEPT} onChange={elegirArchivos} className="sr-only" aria-label="Seleccionar archivos adjuntos" />
              <p className="mt-1 text-xs text-slate-500">Capturas, PDF, documentos de Office, TXT o CSV. Hasta 5 MB cada uno.</p>
              {erroresArchivos.length > 0 && (
                <ul role="alert" className="mt-2 space-y-0.5 text-sm text-rose-600">
                  {erroresArchivos.map((m) => <li key={m}>{m}</li>)}
                </ul>
              )}
            </div>

            {error && (
              <div role="alert" className="rounded-lg bg-rose-50 px-3 py-2.5 text-sm text-rose-700 ring-1 ring-rose-200">{error}</div>
            )}

            <div className="flex justify-end">
              <Button type="submit" size="lg" disabled={!valido || enviando}>
                <Send size={15} /> {enviando ? (progreso || 'Registrando...') : 'Registrar ticket'}
              </Button>
            </div>
          </form>
        </Card>
      )}
    </div>
  );
}
