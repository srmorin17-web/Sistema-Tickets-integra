import { useEffect, useState } from 'react';
import {
  AlertCircle, Building2, CalendarClock, CheckCircle2, Clock, Flag, MessageSquare, PlayCircle, Tag, User as UserIcon,
} from 'lucide-react';
import { responderArea } from '../api/areaApi';
import { cambiarEstado, confirmarCierre, enviarRevision, escalarTicket, mensajeDeError, obtenerTicket, responderSolicitudInformacion, solicitarReapertura } from '../api/ticketsApi';
import { useCatalogos } from '../context/CatalogosContext';
import { COMENTARIO_OBLIGATORIO, ESTADOS_ESCALABLES, TRANSICIONES, comentarioPorDefecto, etiquetaAccion } from '../lib/flow';
import { formatDate, timeAgo } from '../lib/ui';
import Adjuntos from './Adjuntos';
import { Drawer, Modal } from './Overlay';
import Timeline from './Timeline';
import { Button, EstadoBadge, PrioridadBadge, SlaBar, Spinner } from './ui';

const TEXTOS_ESTADO = {
  'Resuelto': {
    titulo: 'Marcar como resuelto',
    ayuda: 'Describa la solución aplicada. El comentario queda registrado en el historial del ticket.',
    placeholder: 'Solución aplicada...',
  },
  'Requiere información': {
    titulo: 'Solicitar información',
    ayuda: 'El ticket pasará a "Requiere información" y el SLA se pausará hasta que vuelva a revisión.',
    placeholder: '¿Qué información necesita del solicitante?',
  },
};

const ICONO_ACCION = {
  'En revisión': PlayCircle,
  'Requiere información': MessageSquare,
  'Resuelto': CheckCircle2,
};

function Meta({ icono, etiqueta, valor }) {
  return (
    <div>
      <p className="flex items-center gap-1.5 text-xs font-medium text-slate-500">{icono}{etiqueta}</p>
      <p className="mt-0.5 text-sm font-medium text-slate-900">{valor}</p>
    </div>
  );
}

/**
 * Detalle de un ticket con su historial y las acciones permitidas para el estado actual.
 * Se monta con `key={ticketId}` para que su estado se reinicie al abrir otro ticket.
 */
export default function TicketDrawer({ ticketId, onClose, onChanged = () => {}, puedeActuar = true, vistaSolicitante = false, permitirRespuestaArea = false, soloLectura = false }) {
  const { catalogos, error: errorCatalogos, recargar: recargarCatalogos } = useCatalogos();

  const [detalle, setDetalle] = useState(null);
  const [errorCarga, setErrorCarga] = useState('');
  const [version, setVersion] = useState(0);

  const [modal, setModal] = useState(null); // { tipo: 'estado', hacia } | { tipo: 'escalar' }
  const [comentario, setComentario] = useState('');
  const [areaId, setAreaId] = useState('');
  const [motivo, setMotivo] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [errorAccion, setErrorAccion] = useState('');
  const [aviso, setAviso] = useState('');
  const [respuestaArea, setRespuestaArea] = useState('');
  const [enviandoRespuesta, setEnviandoRespuesta] = useState(false);
  const [errorRespuesta, setErrorRespuesta] = useState('');
  const [revision, setRevision] = useState('');
  const [enviandoRevision, setEnviandoRevision] = useState(false);
  const [errorRevision, setErrorRevision] = useState('');
  const [mostrarConfirmarCierre, setMostrarConfirmarCierre] = useState(false);
  const [enviandoCierre, setEnviandoCierre] = useState(false);
  const [errorCierre, setErrorCierre] = useState('');
  const [mostrarReapertura, setMostrarReapertura] = useState(false);
  const [motivoReapertura, setMotivoReapertura] = useState('');
  const [enviandoReapertura, setEnviandoReapertura] = useState(false);
  const [errorReapertura, setErrorReapertura] = useState('');
  const [informacionAdicional, setInformacionAdicional] = useState('');
  const [enviandoInformacion, setEnviandoInformacion] = useState(false);
  const [errorInformacion, setErrorInformacion] = useState('');

  useEffect(() => {
    let activo = true;
    obtenerTicket(ticketId)
      .then((datos) => {
        if (!activo) return;
        setDetalle(datos);
        setErrorCarga('');
      })
      .catch((e) => {
        if (activo) setErrorCarga(mensajeDeError(e, 'No se pudo cargar el ticket.'));
      });
    return () => {
      activo = false;
    };
  }, [ticketId, version]);

  const recargarDetalle = () => {
    setErrorCarga('');
    setVersion((v) => v + 1);
  };

  // Espera el detalle actualizado antes de mostrar el aviso, para que los botones reflejen el nuevo estado
  const refrescarDetalle = async () => {
    try {
      setDetalle(await obtenerTicket(ticketId));
    } catch {
      recargarDetalle();
    }
  };

  const cerrarModal = () => {
    if (enviando) return;
    setModal(null);
    setErrorAccion('');
  };

  const ejecutarEstado = async (hacia, texto) => {
    const destino = catalogos?.estados.find((e) => e.nombre === hacia);
    if (!destino) {
      setErrorAccion('Los catálogos del sistema no están disponibles. Intente nuevamente.');
      return;
    }
    setEnviando(true);
    setErrorAccion('');
    try {
      await cambiarEstado(detalle.id, { nuevoEstadoId: destino.id, comentario: texto });
      await refrescarDetalle();
      setModal(null);
      setAviso(`El ticket pasó a "${hacia}".`);
      onChanged();
    } catch (e) {
      setErrorAccion(mensajeDeError(e, 'No se pudo cambiar el estado del ticket.'));
    } finally {
      setEnviando(false);
    }
  };

  const iniciarTransicion = (hacia) => {
    setAviso('');
    setErrorAccion('');
    if (COMENTARIO_OBLIGATORIO.includes(hacia)) {
      setComentario('');
      setModal({ tipo: 'estado', hacia });
    } else {
      ejecutarEstado(hacia, comentarioPorDefecto(detalle.estado, hacia));
    }
  };

  const abrirEscalar = () => {
    setAviso('');
    setErrorAccion('');
    setAreaId('');
    setMotivo('');
    setModal({ tipo: 'escalar' });
  };

  const ejecutarEscalar = async () => {
    setEnviando(true);
    setErrorAccion('');
    try {
      await escalarTicket(detalle.id, { areaEscalamientoId: areaId, motivo: motivo.trim() });
      const area = catalogos.areasEscalamiento.find((a) => a.id === areaId)?.nombre;
      await refrescarDetalle();
      setModal(null);
      setAviso(`Ticket escalado${area ? ` al área ${area}` : ''}.`);
      onChanged();
    } catch (e) {
      const estado = e.response?.status;
      setErrorAccion(
        estado === 422
          ? 'El área seleccionada no tiene un representante asignado. Pida a un administrador que lo configure.'
          : estado === 409
            ? 'El área ya no está activa o el ticket ya finalizó. Actualice la pantalla.'
            : mensajeDeError(e, 'No se pudo escalar el ticket.')
      );
    } finally {
      setEnviando(false);
    }
  };

  const enviarRespuestaArea = async (e) => {
    e.preventDefault();
    if (!respuestaArea.trim() || enviandoRespuesta) return;
    setEnviandoRespuesta(true);
    setErrorRespuesta('');
    try {
      await responderArea(detalle.id, respuestaArea.trim());
      await refrescarDetalle();
      setAviso('Respuesta enviada: el ticket volvió al técnico.');
      onChanged();
    } catch (err) {
      const estado = err.response?.status;
      setErrorRespuesta(
        estado === 403
          ? 'Solo el representante del área puede responder este ticket.'
          : estado === 409
            ? 'El ticket ya no está escalado. Actualice la pantalla.'
            : mensajeDeError(err, 'No se pudo enviar la respuesta. Intente nuevamente.')
      );
    } finally {
      setEnviandoRespuesta(false);
    }
  };

  const enviarComentario = async (e) => {
    e.preventDefault();
    if (!revision.trim() || enviandoRevision) return;
    setEnviandoRevision(true);
    setErrorRevision('');
    try {
      await enviarRevision(detalle.id, revision.trim());
      await refrescarDetalle();
      setAviso('Gracias, su comentario quedó registrado.');
    } catch (err) {
      setErrorRevision(
        err.response?.status === 409
          ? 'Ya registró un comentario para este ticket o el ticket ya no está en estado Resuelto.'
          : mensajeDeError(err, 'No se pudo enviar el comentario. Intente nuevamente.')
      );
    } finally {
      setEnviandoRevision(false);
    }
  };

  const cerrarTicket = async () => {
    if (enviandoCierre) return;
    setEnviandoCierre(true);
    setErrorCierre('');
    try {
      await confirmarCierre(detalle.id);
      setMostrarConfirmarCierre(false);
      await refrescarDetalle();
      setAviso('Solución confirmada. El ticket quedó cerrado.');
      onChanged();
    } catch (err) {
      setErrorCierre(
        err.response?.status === 409
          ? 'El ticket ya no está en estado Resuelto. Actualice la pantalla.'
          : err.response?.status === 403
            ? 'Solo quien creó el ticket puede confirmar el cierre.'
            : mensajeDeError(err, 'No se pudo cerrar el ticket. Intente nuevamente.')
      );
    } finally {
      setEnviandoCierre(false);
    }
  };

  const reabrirTicket = async (e) => {
    e.preventDefault();
    const motivoTexto = motivoReapertura.trim();
    if (!motivoTexto || enviandoReapertura) return;
    setEnviandoReapertura(true);
    setErrorReapertura('');
    try {
      await solicitarReapertura(detalle.id, motivoTexto);
      setMotivoReapertura('');
      setMostrarReapertura(false);
      await refrescarDetalle();
      setAviso('Ticket reabierto y enviado al técnico para continuar la atención.');
      onChanged();
    } catch (err) {
      setErrorReapertura(
        err.response?.status === 409
          ? 'El ticket ya no está en estado Resuelto. Actualice la pantalla.'
          : err.response?.status === 403
            ? 'Solo quien creó el ticket puede solicitar su reapertura.'
            : mensajeDeError(err, 'No se pudo reabrir el ticket. Intente nuevamente.')
      );
    } finally {
      setEnviandoReapertura(false);
    }
  };

  const enviarInformacionAdicional = async (e) => {
    e.preventDefault();
    const texto = informacionAdicional.trim();
    if (!texto || enviandoInformacion) return;
    setEnviandoInformacion(true);
    setErrorInformacion('');
    try {
      await responderSolicitudInformacion(detalle.id, texto);
      setInformacionAdicional('');
      await refrescarDetalle();
      setAviso('Información enviada. El ticket volvió a revisión técnica.');
      onChanged();
    } catch (err) {
      setErrorInformacion(
        err.response?.status === 409
          ? 'El ticket ya no está esperando información. Actualice la pantalla.'
          : err.response?.status === 403
            ? 'Solo quien creó el ticket puede responder esta solicitud.'
            : mensajeDeError(err, 'No se pudo enviar la información. Intente nuevamente.')
      );
    } finally {
      setEnviandoInformacion(false);
    }
  };

  const historial = detalle
    ? [
        {
          tipoAccion: 'CREACION',
          fecha: detalle.fechaCreacion,
          comentario: vistaSolicitante ? 'Usted registró este ticket.' : `Registrado por ${detalle.solicitante}.`,
        },
        ...(detalle.historial ?? []),
      ]
    : [];

  const siguientes = detalle ? TRANSICIONES[detalle.estado] ?? [] : [];
  const puedeEscalar = detalle && ESTADOS_ESCALABLES.includes(detalle.estado);
  const hayAcciones = puedeActuar && !vistaSolicitante && (siguientes.length > 0 || puedeEscalar);
  const finalizado = detalle ? ['Resuelto', 'Cerrado'].includes(detalle.estado) : false;
  const yaComento = detalle?.historial?.some((ev) => ev.tipoAccion === 'REVISION_USUARIO') ?? false;
  const cierreAutomatico = detalle
    ? formatDate(new Date(new Date(detalle.fechaActualizacion).getTime() + 72 * 3600 * 1000).toISOString())
    : '';
  const areaElegida = catalogos?.areasEscalamiento.find((a) => a.id === areaId);
  const textosModal = modal?.tipo === 'estado' ? TEXTOS_ESTADO[modal.hacia] : null;

  return (
    <Drawer
      onClose={onClose}
      title={detalle?.codigo ?? 'Ticket'}
      subtitle={detalle?.asunto ?? (errorCarga ? '' : 'Cargando...')}
    >
      {!detalle && !errorCarga && <Spinner label="Cargando ticket..." />}

      {errorCarga && (
        <div role="alert" className="space-y-3 rounded-lg bg-rose-50 p-4 text-sm text-rose-700 ring-1 ring-rose-200">
          <p>{errorCarga}</p>
          <Button size="sm" variant="outline" onClick={recargarDetalle}>Reintentar</Button>
        </div>
      )}

      {detalle && (
        <div className="space-y-6">
          <div className="flex flex-wrap items-center gap-2">
            <EstadoBadge estado={detalle.estado} />
            <PrioridadBadge prioridad={detalle.prioridad} />
            <span className="text-xs text-slate-500">Creado {timeAgo(detalle.fechaCreacion)}</span>
          </div>

          {vistaSolicitante ? (
            !finalizado && (
              <section className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                <div className="flex items-center gap-2 text-sm font-medium text-slate-700">
                  <Clock size={15} /> Atención estimada
                </div>
                <p className="mt-2 text-sm text-slate-600">
                  {detalle.pausado
                    ? 'El plazo está en pausa mientras el ticket espera información o la respuesta de otra área.'
                    : detalle.fechaVencimientoSla
                      ? `Se prevé atenderlo antes del ${formatDate(detalle.fechaVencimientoSla)}.`
                      : 'Aún no tiene un plazo definido.'}
                </p>
              </section>
            )
          ) : (
          <section className="rounded-xl border border-slate-200 bg-slate-50 p-4">
              <div className="flex items-center gap-2 text-sm font-medium text-slate-700">
                <Clock size={15} /> Acuerdo de nivel de servicio
              </div>
              <div className="mt-3">
                <SlaBar ticket={detalle} />
              </div>
              {detalle.fechaVencimientoSla && (
                <p className="mt-2 text-xs text-slate-500">Vence: {formatDate(detalle.fechaVencimientoSla)}</p>
              )}
              {detalle.pausado && (
                <p className="mt-1 text-xs text-slate-500">El conteo del SLA está en pausa.</p>
              )}
            </section>
          )}

          {vistaSolicitante && detalle.estado === 'Requiere información' && (
            <>
              <div role="status" className="flex items-start gap-2 rounded-lg bg-amber-50 px-3 py-2.5 text-sm text-amber-800 ring-1 ring-amber-200">
                <AlertCircle size={15} className="mt-0.5 shrink-0" />
                <span>El equipo de TI solicitó información adicional. Revise su solicitud y responda aquí para continuar con la atención.</span>
              </div>
              <section className="rounded-xl border border-amber-200 p-4">
                <h3 className="text-sm font-semibold text-slate-900">Enviar información al técnico</h3>
                <p className="mt-1 text-xs text-slate-500">
                  Puede detallar el problema en este mensaje y adjuntar archivos arriba. Al enviarlo, el ticket volverá a revisión técnica.
                </p>
                <form onSubmit={enviarInformacionAdicional} className="mt-3 space-y-3">
                  <label htmlFor="informacion-adicional" className="sr-only">Información adicional</label>
                  <textarea
                    id="informacion-adicional"
                    value={informacionAdicional}
                    onChange={(e) => setInformacionAdicional(e.target.value)}
                    rows={4}
                    maxLength={1000}
                    placeholder="Describa la información solicitada o amplíe los detalles del problema..."
                    className="input resize-none"
                  />
                  {errorInformacion && <p role="alert" className="text-sm text-rose-600">{errorInformacion}</p>}
                  <div className="flex justify-end">
                    <Button type="submit" size="sm" disabled={!informacionAdicional.trim() || enviandoInformacion}>
                      {enviandoInformacion ? 'Enviando...' : 'Enviar información'}
                    </Button>
                  </div>
                </form>
              </section>
            </>
          )}

          <section>
            <h3 className="mb-2 text-sm font-semibold text-slate-900">Descripción</h3>
            <p className="whitespace-pre-wrap text-sm leading-relaxed text-slate-600">{detalle.descripcion}</p>
          </section>

          <Adjuntos ticketId={detalle.id} finalizado={finalizado} soloLectura={soloLectura} onCambio={recargarDetalle} />

          <section className="grid grid-cols-2 gap-4 rounded-xl border border-slate-200 p-4">
            {!vistaSolicitante && (
              <Meta icono={<UserIcon size={14} />} etiqueta="Solicitante" valor={detalle.solicitante} />
            )}
            <Meta icono={<UserIcon size={14} />} etiqueta="Técnico" valor={detalle.tecnicoAsignado ?? 'Sin asignar'} />
            <Meta icono={<Tag size={14} />} etiqueta="Categoría" valor={detalle.categoria} />
            {!vistaSolicitante && (
              <Meta icono={<Building2 size={14} />} etiqueta="Área escalada"
                valor={detalle.areaEscalada ? `${detalle.areaEscalada}${detalle.representanteArea ? ` (${detalle.representanteArea})` : ''}` : '—'} />
            )}
            <Meta icono={<CalendarClock size={14} />} etiqueta="Creado" valor={formatDate(detalle.fechaCreacion)} />
            <Meta icono={<CalendarClock size={14} />} etiqueta="Actualizado" valor={formatDate(detalle.fechaActualizacion)} />
          </section>

          {aviso && (
            <div role="status" className="flex items-center gap-2 rounded-lg bg-emerald-50 px-3 py-2.5 text-sm text-emerald-700 ring-1 ring-emerald-200">
              <CheckCircle2 size={15} /> {aviso}
            </div>
          )}

          {!modal && errorAccion && (
            <div role="alert" className="flex items-center gap-2 rounded-lg bg-rose-50 px-3 py-2.5 text-sm text-rose-700 ring-1 ring-rose-200">
              <AlertCircle size={15} /> {errorAccion}
            </div>
          )}

          {vistaSolicitante && detalle.estado === 'Resuelto' && (
            <section className="rounded-xl border border-slate-200 p-4">
              <h3 className="text-sm font-semibold text-slate-900">¿Quedó conforme con la solución?</h3>
              <p className="mt-1 text-xs text-slate-500">
                Si la solución funciona, puede cerrar el ticket ahora. Si no realiza ninguna acción, se cerrará automáticamente alrededor del {cierreAutomatico}. Si el problema continúa, solicite la reapertura.
              </p>
              {!mostrarConfirmarCierre ? (
                <Button
                  type="button"
                  size="sm"
                  className="mt-3"
                  onClick={() => { setMostrarConfirmarCierre(true); setErrorCierre(''); }}
                >
                  <CheckCircle2 size={15} /> Confirmar solución y cerrar
                </Button>
              ) : (
                <div className="mt-3 rounded-lg bg-emerald-50 p-3 ring-1 ring-emerald-200">
                  <p className="text-sm text-emerald-900">Al confirmar, el ticket se cerrará ahora. ¿Desea continuar?</p>
                  {errorCierre && <p role="alert" className="mt-2 text-sm text-rose-600">{errorCierre}</p>}
                  <div className="mt-3 flex justify-end gap-2">
                    <Button type="button" size="sm" variant="ghost" disabled={enviandoCierre}
                      onClick={() => { setMostrarConfirmarCierre(false); setErrorCierre(''); }}>
                      Cancelar
                    </Button>
                    <Button type="button" size="sm" disabled={enviandoCierre} onClick={cerrarTicket}>
                      {enviandoCierre ? 'Cerrando...' : 'Sí, cerrar ticket'}
                    </Button>
                  </div>
                </div>
              )}
              {yaComento ? (
                <p className="mt-3 text-sm text-slate-600">Ya registró su comentario; puede verlo en el historial.</p>
              ) : (
                <form onSubmit={enviarComentario} className="mt-3 space-y-3">
                  <label htmlFor="revision-comentario" className="sr-only">Comentario sobre la solución</label>
                  <textarea
                    id="revision-comentario"
                    value={revision}
                    onChange={(e) => setRevision(e.target.value)}
                    rows={3}
                    maxLength={1000}
                    placeholder="Cuéntenos si el problema quedó resuelto o qué faltó..."
                    className="input resize-none"
                  />
                  {errorRevision && <p role="alert" className="text-sm text-rose-600">{errorRevision}</p>}
                  <div className="flex justify-end">
                    <Button type="submit" size="sm" disabled={!revision.trim() || enviandoRevision}>
                      {enviandoRevision ? 'Enviando...' : 'Enviar comentario'}
                    </Button>
                  </div>
                </form>
              )}
              {!mostrarReapertura ? (
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  className="mt-3"
                  onClick={() => { setMostrarReapertura(true); setErrorReapertura(''); }}
                >
                  No, el problema continúa
                </Button>
              ) : (
                <form onSubmit={reabrirTicket} className="mt-4 space-y-3 border-t border-slate-200 pt-4">
                  <label htmlFor="motivo-reapertura" className="block text-sm font-medium text-slate-700">
                    ¿Qué problema sigue ocurriendo?
                  </label>
                  <textarea
                    id="motivo-reapertura"
                    value={motivoReapertura}
                    onChange={(e) => setMotivoReapertura(e.target.value)}
                    rows={4}
                    maxLength={1000}
                    placeholder="Describa por qué la solución no resolvió el problema..."
                    className="input resize-none"
                    required
                  />
                  {errorReapertura && <p role="alert" className="text-sm text-rose-600">{errorReapertura}</p>}
                  <div className="flex justify-end gap-2">
                    <Button type="button" size="sm" variant="ghost" disabled={enviandoReapertura}
                      onClick={() => { setMostrarReapertura(false); setErrorReapertura(''); }}>
                      Cancelar
                    </Button>
                    <Button type="submit" size="sm" disabled={!motivoReapertura.trim() || enviandoReapertura}>
                      {enviandoReapertura ? 'Reabriendo...' : 'Enviar y reabrir ticket'}
                    </Button>
                  </div>
                </form>
              )}
            </section>
          )}

          {permitirRespuestaArea && detalle.estado === 'Escalado' && (
            <section className="rounded-xl border border-slate-200 p-4">
              <h3 className="text-sm font-semibold text-slate-900">Responder al técnico</h3>
              <p className="mt-1 text-xs text-slate-500">
                Su respuesta devuelve el ticket a {detalle.tecnicoAsignado ?? 'la bandeja del equipo de TI'} y reanuda el SLA.
                {!detalle.tecnicoAsignado && ' Como no tiene técnico asignado, un supervisor deberá asignarlo.'}
              </p>
              <form onSubmit={enviarRespuestaArea} className="mt-3 space-y-3">
                <label htmlFor="respuesta-area" className="sr-only">Respuesta del área</label>
                <textarea id="respuesta-area" value={respuestaArea} onChange={(e) => setRespuestaArea(e.target.value)}
                  rows={4} maxLength={1000} className="input resize-none"
                  placeholder="Describa qué se hizo o qué debe hacer el técnico a continuación..." />
                {errorRespuesta && <p role="alert" className="text-sm text-rose-600">{errorRespuesta}</p>}
                <div className="flex justify-end">
                  <Button type="submit" size="sm" disabled={!respuestaArea.trim() || enviandoRespuesta}>
                    {enviandoRespuesta ? 'Enviando...' : 'Enviar respuesta'}
                  </Button>
                </div>
              </form>
            </section>
          )}

          {hayAcciones && (
            <section aria-label="Acciones del ticket" className="space-y-2">
              {errorCatalogos && (
                <div role="alert" className="flex items-center justify-between gap-2 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-800 ring-1 ring-amber-200">
                  <span>{errorCatalogos}</span>
                  <Button size="sm" variant="outline" onClick={recargarCatalogos}>Reintentar</Button>
                </div>
              )}
              <div className="flex flex-wrap gap-2">
                {siguientes.map((hacia) => {
                  const Icono = ICONO_ACCION[hacia] ?? CheckCircle2;
                  return (
                    <Button
                      key={hacia}
                      size="sm"
                      variant={hacia === 'Requiere información' ? 'outline' : 'primary'}
                      disabled={enviando || !catalogos}
                      onClick={() => iniciarTransicion(hacia)}
                    >
                      <Icono size={14} /> {etiquetaAccion(detalle.estado, hacia)}
                    </Button>
                  );
                })}
                {puedeEscalar && (
                  <Button size="sm" variant="outline" disabled={enviando || !catalogos} onClick={abrirEscalar}>
                    <Flag size={14} /> Escalar
                  </Button>
                )}
              </div>
            </section>
          )}

          <section>
            <h3 className="mb-4 text-sm font-semibold text-slate-900">Historial ({historial.length})</h3>
            <Timeline eventos={historial} vistaSolicitante={vistaSolicitante} />
          </section>
        </div>
      )}

      {modal?.tipo === 'estado' && textosModal && (
        <Modal
          onClose={cerrarModal}
          title={textosModal.titulo}
          footer={
            <div className="flex justify-end gap-2">
              <Button variant="ghost" onClick={cerrarModal} disabled={enviando}>Cancelar</Button>
              <Button onClick={() => ejecutarEstado(modal.hacia, comentario.trim())} disabled={enviando || !comentario.trim()}>
                {enviando ? 'Guardando...' : textosModal.titulo}
              </Button>
            </div>
          }
        >
          <div className="space-y-3">
            <p className="text-sm text-slate-600">{textosModal.ayuda}</p>
            <label htmlFor="comentario-estado" className="sr-only">Comentario</label>
            <textarea
              id="comentario-estado"
              value={comentario}
              onChange={(e) => setComentario(e.target.value)}
              rows={4}
              maxLength={1000}
              placeholder={textosModal.placeholder}
              className="input resize-none"
              autoFocus
            />
            {errorAccion && <p role="alert" className="text-sm text-rose-600">{errorAccion}</p>}
          </div>
        </Modal>
      )}

      {modal?.tipo === 'escalar' && (
        <Modal
          onClose={cerrarModal}
          title="Escalar ticket"
          footer={
            <div className="flex justify-end gap-2">
              <Button variant="ghost" onClick={cerrarModal} disabled={enviando}>Cancelar</Button>
              <Button onClick={ejecutarEscalar} disabled={enviando || !areaId || !motivo.trim()}>
                <Flag size={15} /> {enviando ? 'Escalando...' : 'Escalar'}
              </Button>
            </div>
          }
        >
          <div className="space-y-4">
            <p className="text-sm text-slate-600">
              El ticket pasará a "Escalado" y el SLA se pausará mientras el área destino lo atiende.
            </p>
            <div>
              <label htmlFor="area-escalar" className="mb-1.5 block text-sm font-medium text-slate-700">Área destino</label>
              <select id="area-escalar" value={areaId} onChange={(e) => setAreaId(e.target.value)} className="input">
                <option value="">Seleccione un área...</option>
                {(catalogos?.areasEscalamiento ?? []).map((a) => (
                  <option key={a.id} value={a.id} disabled={!a.representante}>
                    {a.nombre}{a.representante ? ` (${a.representante})` : ' (sin representante)'}
                  </option>
                ))}
              </select>
              {areaElegida?.representante && (
                <p className="mt-1.5 text-xs text-slate-600">
                  Se avisará por correo a {areaElegida.representante}, con usted en copia.
                </p>
              )}
              {catalogos && catalogos.areasEscalamiento.length === 0 && (
                <p className="mt-1.5 text-xs text-slate-500">No hay áreas de escalamiento activas. Un administrador debe registrarlas.</p>
              )}
            </div>
            <div>
              <label htmlFor="motivo-escalar" className="mb-1.5 block text-sm font-medium text-slate-700">Motivo del escalamiento</label>
              <textarea
                id="motivo-escalar"
                value={motivo}
                onChange={(e) => setMotivo(e.target.value)}
                rows={3}
                maxLength={1000}
                placeholder="Explique por qué debe atenderlo otra área..."
                className="input resize-none"
              />
            </div>
            {errorAccion && <p role="alert" className="text-sm text-rose-600">{errorAccion}</p>}
          </div>
        </Modal>
      )}
    </Drawer>
  );
}
