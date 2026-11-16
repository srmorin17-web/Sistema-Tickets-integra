import { ArrowLeftRight, ArrowRight, Flag, MessageSquare, Paperclip, Plus, Trash2 } from 'lucide-react';
import { formatDate, timeAgo } from '../lib/ui';

const TIPOS = {
  CREACION: { icono: Plus, color: 'bg-emerald-50 text-emerald-600', titulo: 'Ticket creado' },
  CAMBIO_ESTADO: { icono: ArrowRight, color: 'bg-sky-50 text-sky-600', titulo: 'Cambio de estado' },
  ESCALAMIENTO: { icono: Flag, color: 'bg-violet-50 text-violet-600', titulo: 'Escalamiento' },
  ADJUNTO_AGREGADO: { icono: Paperclip, color: 'bg-sky-50 text-sky-600', titulo: 'Archivo adjuntado' },
  ADJUNTO_ELIMINADO: { icono: Trash2, color: 'bg-slate-100 text-slate-600', titulo: 'Archivo eliminado' },
  REASIGNACION: { icono: ArrowLeftRight, color: 'bg-sky-50 text-sky-600', titulo: 'Reasignación' },
  REVISION_USUARIO: { icono: MessageSquare, color: 'bg-amber-50 text-amber-600', titulo: 'Comentario del solicitante' },
};
const GENERICO = { icono: MessageSquare, color: 'bg-slate-100 text-slate-600', titulo: 'Actualización' };

/**
 * Historial cronológico. Muestra quién ejecutó cada acción cuando el backend lo informa
 * (el solicitante no recibe el nombre del personal de TI).
 */
export default function Timeline({ eventos, vistaSolicitante = false }) {
  return (
    <ol className="space-y-1">
      {eventos.map((ev, i) => {
        const tipo = TIPOS[ev.tipoAccion] ?? GENERICO;
        const Icono = tipo.icono;
        let titulo = tipo.titulo;
        if (ev.tipoAccion === 'CAMBIO_ESTADO' && ev.estadoNuevo === 'Requiere información') {
          titulo = 'Información solicitada';
        }
        if (ev.tipoAccion === 'CAMBIO_ESTADO'
            && ev.estadoAnterior === 'Requiere información'
            && ev.estadoNuevo === 'En revisión') {
          titulo = vistaSolicitante ? 'Su respuesta' : 'Información recibida del solicitante';
        }
        if (ev.tipoAccion === 'CAMBIO_ESTADO'
            && ev.estadoAnterior === 'Resuelto'
            && ev.estadoNuevo === 'En revisión') {
          titulo = vistaSolicitante ? 'Usted solicitó la reapertura' : 'Ticket reabierto por el solicitante';
        }
        if (ev.tipoAccion === 'CAMBIO_ESTADO'
            && ev.estadoAnterior === 'Resuelto'
            && ev.estadoNuevo === 'Cerrado'
            && ev.comentario === 'Solución confirmada y aceptada por el solicitante.') {
          titulo = 'Solución confirmada y ticket cerrado';
        }
        if (ev.tipoAccion === 'ESCALAMIENTO' && ev.area) titulo = `Escalado a ${ev.area}`;
        if (ev.tipoAccion === 'REASIGNACION' && ev.tecnico) titulo = `Reasignado a ${ev.tecnico}`;
        if (ev.tipoAccion === 'REVISION_USUARIO' && vistaSolicitante) titulo = 'Su comentario';
        return (
          <li key={`${ev.tipoAccion}-${ev.fecha}-${i}`} className="relative flex gap-3 pb-5">
            {i < eventos.length - 1 && <div className="absolute left-[15px] top-8 h-full w-px bg-slate-200" />}
            <div className={`relative z-10 flex h-8 w-8 shrink-0 items-center justify-center rounded-full ${tipo.color}`}>
              <Icono size={15} />
            </div>
            <div className="min-w-0 flex-1 pt-0.5">
              <div className="flex flex-wrap items-center gap-x-2">
                <span className="text-sm font-medium text-slate-900">{titulo}</span>
                {ev.usuario && <span className="text-xs text-slate-500">por {ev.usuario}</span>}
                <time dateTime={ev.fecha} title={formatDate(ev.fecha)} className="text-xs text-slate-500">
                  {timeAgo(ev.fecha)}
                </time>
              </div>
              {ev.comentario && <p className="mt-1 whitespace-pre-wrap text-sm text-slate-600">{ev.comentario}</p>}
              {ev.estadoAnterior && ev.estadoNuevo && (
                <div className="mt-1.5 inline-flex items-center gap-1.5 text-xs text-slate-500">
                  <span className="rounded bg-slate-100 px-1.5 py-0.5">{ev.estadoAnterior}</span>
                  <ArrowRight size={11} />
                  <span className="rounded bg-slate-100 px-1.5 py-0.5">{ev.estadoNuevo}</span>
                </div>
              )}
            </div>
          </li>
        );
      })}
    </ol>
  );
}
