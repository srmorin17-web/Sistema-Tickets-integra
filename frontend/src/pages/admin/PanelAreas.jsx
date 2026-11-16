import { useState } from 'react';
import { Pencil, Plus } from 'lucide-react';
import { actualizarArea, crearArea } from '../../api/adminApi';
import { Modal } from '../../components/Overlay';
import SelectorEmpleado from '../../components/SelectorEmpleado';
import { Button, Card } from '../../components/ui';
import { mensajeDeGuardado } from '../../lib/errores';
import { CabeceraPanel, Campo, EstadoActivo } from './comunes';

function FormularioArea({ area, onCerrar, onGuardado }) {
  const esNueva = !area;
  const [nombre, setNombre] = useState(area?.nombre ?? '');
  const [descripcion, setDescripcion] = useState(area?.descripcion ?? '');
  const [activo, setActivo] = useState(area?.activo ?? true);
  const [representante, setRepresentante] = useState(area?.representante ?? null);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');

  const guardar = async (e) => {
    e.preventDefault();
    if (!nombre.trim() || enviando) return;
    setEnviando(true);
    setError('');
    const cuerpo = {
      nombre: nombre.trim(),
      descripcion: descripcion.trim(),
      activo,
      contactoResponsableId: representante?.id ?? null,
    };
    try {
      if (esNueva) await crearArea(cuerpo);
      else await actualizarArea(area.id, cuerpo);
      onGuardado(esNueva ? 'Área creada.' : 'Área actualizada.');
    } catch (err) {
      setError(mensajeDeGuardado(err, {
        reglaNegocio: 'No se puede desactivar el área: tiene tickets activos escalados. Resuélvalos primero.',
      }));
      setEnviando(false);
    }
  };

  return (
    <Modal
      onClose={enviando ? () => {} : onCerrar}
      title={esNueva ? 'Nueva área de escalamiento' : 'Editar área de escalamiento'}
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onCerrar} disabled={enviando}>Cancelar</Button>
          <Button type="submit" form="form-area" disabled={!nombre.trim() || enviando}>
            {enviando ? 'Guardando...' : 'Guardar'}
          </Button>
        </div>
      }
    >
      <form id="form-area" onSubmit={guardar} className="space-y-4">
        <Campo id="area-nombre" etiqueta="Nombre">
          <input id="area-nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} maxLength={150} className="input" required autoFocus />
        </Campo>
        <Campo id="area-desc" etiqueta="Descripción (opcional)">
          <textarea id="area-desc" value={descripcion} onChange={(e) => setDescripcion(e.target.value)} maxLength={500} rows={3} className="input resize-none" />
        </Campo>
        <Campo id="area-representante" etiqueta="Representante del área"
          ayuda="Empleado de RR. HH. que recibe por correo los tickets escalados a esta área. Sin representante no se puede escalar a ella.">
          <SelectorEmpleado id="area-representante" seleccionado={representante} onChange={setRepresentante} />
        </Campo>
        {!esNueva && (
          <label className="flex items-start gap-2 text-sm text-slate-700">
            <input type="checkbox" checked={activo} onChange={(e) => setActivo(e.target.checked)} className="mt-0.5 h-4 w-4 rounded border-slate-300 text-sky-600" />
            <span>
              Área activa
              <span className="block text-xs text-slate-500">Un área con tickets activos escalados no puede desactivarse.</span>
            </span>
          </label>
        )}
        {error && <p role="alert" className="text-sm text-rose-600">{error}</p>}
      </form>
    </Modal>
  );
}

export default function PanelAreas({ areas, onGuardado }) {
  const [edicion, setEdicion] = useState(null);

  const guardado = (mensaje) => {
    setEdicion(null);
    onGuardado(mensaje);
  };

  return (
    <Card className="overflow-hidden">
      <CabeceraPanel
        titulo="Áreas de escalamiento"
        nota="Destinos a los que el personal técnico puede escalar un ticket. Cada área tiene un representante (empleado de RR. HH.) que recibe el aviso. Las inactivas o sin representante no aparecen disponibles al escalar."
        accion={<Button size="sm" onClick={() => setEdicion('nueva')}><Plus size={14} /> Nueva área</Button>}
      />
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
            <tr>
              <th scope="col" className="px-5 py-3 font-medium">Nombre</th>
              <th scope="col" className="px-5 py-3 font-medium">Representante</th>
              <th scope="col" className="px-5 py-3 font-medium">Estado</th>
              <th scope="col" className="px-5 py-3"><span className="sr-only">Acciones</span></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {areas.map((a) => (
              <tr key={a.id} className={a.activo ? '' : 'text-slate-400'}>
                <td className="px-5 py-3">
                  <p className={`font-medium ${a.activo ? 'text-slate-900' : 'text-slate-400'}`}>{a.nombre}</p>
                  {a.descripcion && <p className="text-xs text-slate-500">{a.descripcion}</p>}
                </td>
                <td className="px-5 py-3">
                  {a.representante ? (
                    <>
                      <p className="text-sm text-slate-900">{a.representante.nombre}</p>
                      <p className="text-xs text-slate-500">{[a.representante.cargo, a.representante.correo].filter(Boolean).join(' · ')}</p>
                    </>
                  ) : (
                    <span className="text-xs font-medium text-amber-700">Sin representante{a.activo ? ': no se puede escalar a esta área' : ''}</span>
                  )}
                </td>
                <td className="px-5 py-3"><EstadoActivo activo={a.activo} /></td>
                <td className="px-5 py-3 text-right">
                  <Button size="sm" variant="ghost" onClick={() => setEdicion(a)} aria-label={`Editar ${a.nombre}`}>
                    <Pencil size={14} /> Editar
                  </Button>
                </td>
              </tr>
            ))}
            {areas.length === 0 && (
              <tr><td colSpan={4} className="px-5 py-10 text-center text-sm text-slate-500">No hay áreas de escalamiento registradas.</td></tr>
            )}
          </tbody>
        </table>
      </div>
      {edicion && (
        <FormularioArea key={edicion === 'nueva' ? 'nueva' : edicion.id} area={edicion === 'nueva' ? null : edicion}
          onCerrar={() => setEdicion(null)} onGuardado={guardado} />
      )}
    </Card>
  );
}
