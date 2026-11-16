import { useEffect, useRef, useState } from 'react';
import { Download, File as IconoArchivo, FileImage, FileSpreadsheet, FileText, Paperclip, Trash2, Upload } from 'lucide-react';
import { descargarAdjunto, eliminarAdjunto, listarAdjuntos, subirAdjunto } from '../api/adjuntosApi';
import { ACCEPT, MAX_ADJUNTOS, formatBytes, mensajeDeAdjunto, validarArchivo } from '../lib/adjuntos';
import { timeAgo } from '../lib/ui';
import { Button, Spinner } from './ui';

function iconoPara(tipo) {
  if (tipo.startsWith('image/')) return FileImage;
  if (tipo.includes('spreadsheet') || tipo.includes('excel') || tipo === 'text/csv') return FileSpreadsheet;
  if (tipo === 'application/pdf' || tipo.startsWith('text/') || tipo.includes('word') || tipo.includes('msword')) return FileText;
  return IconoArchivo;
}

/**
 * Archivos adjuntos de un ticket: lista, descarga, eliminación y subida (con arrastrar y soltar).
 * `finalizado`: con el ticket Resuelto o Cerrado no se agregan ni se eliminan archivos.
 * `onCambio`: se invoca tras subir o eliminar, para que el historial del ticket se refresque.
 */
export default function Adjuntos({ ticketId, finalizado = false, soloLectura = false, onCambio = () => {} }) {
  const [adjuntos, setAdjuntos] = useState(null);
  const [errorLista, setErrorLista] = useState('');
  const [version, setVersion] = useState(0);
  const [subiendo, setSubiendo] = useState(null); // { nombre, pct }
  const [mensajes, setMensajes] = useState([]);
  const [confirmando, setConfirmando] = useState(null);
  const [arrastrando, setArrastrando] = useState(false);
  const entrada = useRef(null);

  useEffect(() => {
    let activo = true;
    listarAdjuntos(ticketId)
      .then((lista) => {
        if (!activo) return;
        setAdjuntos(lista);
        setErrorLista('');
      })
      .catch((e) => activo && setErrorLista(mensajeDeAdjunto(e)));
    return () => {
      activo = false;
    };
  }, [ticketId, version]);

  const refrescar = () => setVersion((v) => v + 1);
  const vigentes = adjuntos ? adjuntos.filter((a) => !a.purgado).length : 0;

  const subir = async (archivos) => {
    const errores = [];
    let contados = vigentes;
    let subioAlguno = false;
    for (const archivo of archivos) {
      const motivo = validarArchivo(archivo, contados);
      if (motivo) {
        errores.push(motivo);
        continue;
      }
      try {
        setSubiendo({ nombre: archivo.name, pct: 0 });
        await subirAdjunto(ticketId, archivo, (pct) => setSubiendo({ nombre: archivo.name, pct }));
        contados += 1;
        subioAlguno = true;
      } catch (e) {
        errores.push(`"${archivo.name}": ${mensajeDeAdjunto(e)}`);
      }
    }
    setSubiendo(null);
    setMensajes(errores);
    if (subioAlguno) {
      refrescar();
      onCambio();
    }
  };

  const alElegir = (e) => {
    const elegidos = Array.from(e.target.files ?? []);
    e.target.value = ''; // permite elegir de nuevo el mismo archivo
    if (elegidos.length) subir(elegidos);
  };

  const alSoltar = (e) => {
    e.preventDefault();
    setArrastrando(false);
    if (finalizado || soloLectura || subiendo) return;
    const soltados = Array.from(e.dataTransfer.files ?? []);
    if (soltados.length) subir(soltados);
  };

  const descargar = async (adjunto) => {
    setMensajes([]);
    try {
      await descargarAdjunto(adjunto);
    } catch (e) {
      setMensajes([`"${adjunto.nombre}": ${mensajeDeAdjunto(e)}`]);
    }
  };

  const eliminar = async (adjunto) => {
    setMensajes([]);
    try {
      await eliminarAdjunto(adjunto.id);
      setConfirmando(null);
      refrescar();
      onCambio();
    } catch (e) {
      setConfirmando(null);
      setMensajes([`"${adjunto.nombre}": ${mensajeDeAdjunto(e)}`]);
    }
  };

  return (
    <section aria-label="Archivos adjuntos">
      <div className="mb-2 flex items-center justify-between">
        <h3 className="flex items-center gap-1.5 text-sm font-semibold text-slate-900">
          <Paperclip size={14} /> Adjuntos
        </h3>
        {adjuntos && <span className="text-xs text-slate-500">{vigentes} de {MAX_ADJUNTOS}</span>}
      </div>

      {!adjuntos && !errorLista && <Spinner label="Cargando archivos..." />}
      {errorLista && (
        <p role="alert" className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700 ring-1 ring-rose-200">
          {errorLista} <button type="button" onClick={refrescar} className="font-medium underline">Reintentar</button>
        </p>
      )}

      {adjuntos && adjuntos.length === 0 && <p className="text-sm text-slate-500">Este ticket no tiene archivos adjuntos.</p>}

      {adjuntos && adjuntos.length > 0 && (
        <ul className="divide-y divide-slate-100 rounded-xl border border-slate-200">
          {adjuntos.map((a) => {
            const Icono = iconoPara(a.tipo);
            return (
              <li key={a.id} className="flex items-center gap-3 px-3 py-2.5">
                <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-slate-500">
                  <Icono size={17} />
                </div>
                <div className="min-w-0 flex-1">
                  <p className={`truncate text-sm font-medium ${a.purgado ? 'text-slate-400 line-through' : 'text-slate-900'}`} title={a.nombre}>
                    {a.nombre}
                  </p>
                  <p className="truncate text-xs text-slate-500">
                    {a.purgado
                      ? 'Eliminado por la política de retención'
                      : `${formatBytes(a.tamano)}${a.subidoPor ? ` · ${a.subidoPor}` : ''} · ${timeAgo(a.fecha)}`}
                  </p>
                </div>
                {!a.purgado && (
                  <div className="flex shrink-0 items-center gap-1">
                    <Button size="sm" variant="ghost" onClick={() => descargar(a)} aria-label={`Descargar ${a.nombre}`}>
                      <Download size={15} />
                    </Button>
                    {!soloLectura && a.puedeEliminar && (confirmando === a.id ? (
                      <>
                        <Button size="sm" variant="danger" onClick={() => eliminar(a)}>Eliminar</Button>
                        <Button size="sm" variant="ghost" onClick={() => setConfirmando(null)}>Cancelar</Button>
                      </>
                    ) : (
                      <Button size="sm" variant="ghost" onClick={() => setConfirmando(a.id)} aria-label={`Eliminar ${a.nombre}`}>
                        <Trash2 size={15} />
                      </Button>
                    ))}
                  </div>
                )}
              </li>
            );
          })}
        </ul>
      )}

      {adjuntos && !finalizado && !soloLectura && vigentes < MAX_ADJUNTOS && (
        <div
          onDragOver={(e) => { e.preventDefault(); setArrastrando(true); }}
          onDragLeave={() => setArrastrando(false)}
          onDrop={alSoltar}
          className={`mt-3 rounded-xl border-2 border-dashed px-4 py-4 text-center transition-colors ${
            arrastrando ? 'border-sky-500 bg-sky-50' : 'border-slate-300'
          }`}
        >
          {subiendo ? (
            <div role="status" aria-live="polite">
              <p className="truncate text-sm text-slate-700">Subiendo {subiendo.nombre}...</p>
              <div className="mx-auto mt-2 h-1.5 w-48 overflow-hidden rounded-full bg-slate-100">
                <div className="h-full rounded-full bg-sky-600 transition-all" style={{ width: `${subiendo.pct}%` }} />
              </div>
            </div>
          ) : (
            <>
              <Upload size={18} className="mx-auto text-slate-400" />
              <p className="mt-1 text-sm text-slate-600">
                Arrastre archivos aquí o{' '}
                <button type="button" onClick={() => entrada.current?.click()} className="font-medium text-sky-700 underline">
                  seleccione desde su equipo
                </button>
              </p>
              <p className="mt-0.5 text-xs text-slate-500">Hasta 5 MB por archivo. PDF, imágenes, Office, TXT y CSV.</p>
            </>
          )}
          <input ref={entrada} type="file" multiple accept={ACCEPT} onChange={alElegir} className="sr-only" aria-label="Seleccionar archivos" />
        </div>
      )}

      {adjuntos && !finalizado && !soloLectura && vigentes >= MAX_ADJUNTOS && (
        <p className="mt-2 text-xs text-slate-500">Se alcanzó el máximo de {MAX_ADJUNTOS} archivos. Elimine uno para agregar otro.</p>
      )}

      {mensajes.length > 0 && (
        <ul role="alert" className="mt-2 space-y-1 rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700 ring-1 ring-rose-200">
          {mensajes.map((m) => <li key={m}>{m}</li>)}
        </ul>
      )}
    </section>
  );
}
