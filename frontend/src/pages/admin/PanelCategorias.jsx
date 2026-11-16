import { useState } from 'react';
import { Pencil, Plus } from 'lucide-react';
import { actualizarCategoria, crearCategoria } from '../../api/adminApi';
import { Modal } from '../../components/Overlay';
import { Button, Card, PrioridadBadge } from '../../components/ui';
import { mensajeDeGuardado } from '../../lib/errores';
import { CabeceraPanel, Campo, EstadoActivo } from './comunes';

function FormularioCategoria({ categoria, prioridades, onCerrar, onGuardado }) {
  const esNueva = !categoria;
  const [nombre, setNombre] = useState(categoria?.nombre ?? '');
  const [descripcion, setDescripcion] = useState(categoria?.descripcion ?? '');
  const [prioridadId, setPrioridadId] = useState(categoria?.prioridadDefectoId ?? '');
  const [activo, setActivo] = useState(categoria?.activo ?? true);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');

  const valido = nombre.trim() && prioridadId;

  const guardar = async (e) => {
    e.preventDefault();
    if (!valido || enviando) return;
    setEnviando(true);
    setError('');
    const cuerpo = { nombre: nombre.trim(), descripcion: descripcion.trim(), activo, prioridadDefectoId: prioridadId };
    try {
      if (esNueva) await crearCategoria(cuerpo);
      else await actualizarCategoria(categoria.id, cuerpo);
      onGuardado(esNueva ? 'Categoría creada.' : 'Categoría actualizada.');
    } catch (err) {
      setError(mensajeDeGuardado(err));
      setEnviando(false);
    }
  };

  return (
    <Modal
      onClose={enviando ? () => {} : onCerrar}
      title={esNueva ? 'Nueva categoría' : 'Editar categoría'}
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onCerrar} disabled={enviando}>Cancelar</Button>
          <Button type="submit" form="form-categoria" disabled={!valido || enviando}>
            {enviando ? 'Guardando...' : 'Guardar'}
          </Button>
        </div>
      }
    >
      <form id="form-categoria" onSubmit={guardar} className="space-y-4">
        <Campo id="cat-nombre" etiqueta="Nombre">
          <input id="cat-nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} maxLength={100} className="input" required autoFocus />
        </Campo>
        <Campo id="cat-desc" etiqueta="Descripción (opcional)">
          <textarea id="cat-desc" value={descripcion} onChange={(e) => setDescripcion(e.target.value)} maxLength={500} rows={3} className="input resize-none" />
        </Campo>
        <Campo id="cat-prioridad" etiqueta="Prioridad por defecto"
          ayuda="Los tickets nuevos de esta categoría recibirán esta prioridad y su SLA.">
          <select id="cat-prioridad" value={prioridadId} onChange={(e) => setPrioridadId(e.target.value)} className="input" required>
            <option value="">Seleccione una prioridad...</option>
            {prioridades.map((p) => (
              <option key={p.id} value={p.id}>{p.nombre} ({p.slaHoras} h)</option>
            ))}
          </select>
        </Campo>
        {!esNueva && (
          <label className="flex items-start gap-2 text-sm text-slate-700">
            <input type="checkbox" checked={activo} onChange={(e) => setActivo(e.target.checked)} className="mt-0.5 h-4 w-4 rounded border-slate-300 text-sky-600" />
            <span>
              Categoría activa
              <span className="block text-xs text-slate-500">Desactivarla no borra el historial; solo impide usarla en tickets nuevos.</span>
            </span>
          </label>
        )}
        {error && <p role="alert" className="text-sm text-rose-600">{error}</p>}
      </form>
    </Modal>
  );
}

export default function PanelCategorias({ categorias, prioridades, onGuardado }) {
  const [edicion, setEdicion] = useState(null); // null | 'nueva' | categoría

  const guardado = (mensaje) => {
    setEdicion(null);
    onGuardado(mensaje);
  };

  return (
    <Card className="overflow-hidden">
      <CabeceraPanel
        titulo="Categorías"
        nota="Clasifican los tickets y determinan su prioridad. Las inactivas no aparecen al registrar tickets nuevos."
        accion={<Button size="sm" onClick={() => setEdicion('nueva')}><Plus size={14} /> Nueva categoría</Button>}
      />
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
            <tr>
              <th scope="col" className="px-5 py-3 font-medium">Nombre</th>
              <th scope="col" className="px-5 py-3 font-medium">Prioridad por defecto</th>
              <th scope="col" className="px-5 py-3 font-medium">Estado</th>
              <th scope="col" className="px-5 py-3"><span className="sr-only">Acciones</span></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {categorias.map((c) => (
              <tr key={c.id} className={c.activo ? '' : 'text-slate-400'}>
                <td className="px-5 py-3">
                  <p className={`font-medium ${c.activo ? 'text-slate-900' : 'text-slate-400'}`}>{c.nombre}</p>
                  {c.descripcion && <p className="text-xs text-slate-500">{c.descripcion}</p>}
                </td>
                <td className="px-5 py-3">
                  {c.prioridadDefecto ? <PrioridadBadge prioridad={c.prioridadDefecto} /> : <span className="text-xs text-amber-600">Sin asignar</span>}
                </td>
                <td className="px-5 py-3"><EstadoActivo activo={c.activo} /></td>
                <td className="px-5 py-3 text-right">
                  <Button size="sm" variant="ghost" onClick={() => setEdicion(c)} aria-label={`Editar ${c.nombre}`}>
                    <Pencil size={14} /> Editar
                  </Button>
                </td>
              </tr>
            ))}
            {categorias.length === 0 && (
              <tr><td colSpan={4} className="px-5 py-10 text-center text-sm text-slate-500">No hay categorías registradas.</td></tr>
            )}
          </tbody>
        </table>
      </div>
      {edicion && (
        <FormularioCategoria
          key={edicion === 'nueva' ? 'nueva' : edicion.id}
          categoria={edicion === 'nueva' ? null : edicion}
          prioridades={prioridades}
          onCerrar={() => setEdicion(null)}
          onGuardado={guardado}
        />
      )}
    </Card>
  );
}
