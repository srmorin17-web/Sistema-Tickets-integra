import { useState } from 'react';
import { Pencil } from 'lucide-react';
import { actualizarPrioridad } from '../../api/adminApi';
import { Modal } from '../../components/Overlay';
import { Button, Card, PrioridadBadge } from '../../components/ui';
import { mensajeDeGuardado } from '../../lib/errores';
import { CabeceraPanel, Campo } from './comunes';

const SLA_MIN = 1;
const SLA_MAX = 720;

function FormularioPrioridad({ prioridad, onCerrar, onGuardado }) {
  const [horas, setHoras] = useState(String(prioridad.slaHoras));
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');

  const valor = Number(horas);
  const valido = Number.isInteger(valor) && valor >= SLA_MIN && valor <= SLA_MAX;

  const guardar = async (e) => {
    e.preventDefault();
    if (!valido || enviando) return;
    setEnviando(true);
    setError('');
    try {
      await actualizarPrioridad(prioridad.id, { nombre: prioridad.nombre, slaHoras: valor });
      onGuardado(`SLA de la prioridad ${prioridad.nombre} actualizado.`);
    } catch (err) {
      setError(mensajeDeGuardado(err));
      setEnviando(false);
    }
  };

  return (
    <Modal
      onClose={enviando ? () => {} : onCerrar}
      title={`Editar prioridad ${prioridad.nombre}`}
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onCerrar} disabled={enviando}>Cancelar</Button>
          <Button type="submit" form="form-prioridad" disabled={!valido || enviando}>
            {enviando ? 'Guardando...' : 'Guardar'}
          </Button>
        </div>
      }
    >
      <form id="form-prioridad" onSubmit={guardar} className="space-y-4">
        <Campo id="pri-nombre" etiqueta="Nombre" ayuda="El nombre no se puede modificar: el sistema lo usa en reglas como la alerta de prioridad Crítica.">
          <input id="pri-nombre" value={prioridad.nombre} disabled className="input bg-slate-50 text-slate-500" />
        </Campo>
        <Campo id="pri-sla" etiqueta="Tiempo de SLA (horas)"
          ayuda={`Entre ${SLA_MIN} y ${SLA_MAX} horas, contadas en horario laboral. Solo se aplica a los tickets nuevos.`}>
          <input id="pri-sla" type="number" inputMode="numeric" min={SLA_MIN} max={SLA_MAX} step={1} value={horas}
            onChange={(e) => setHoras(e.target.value)} className="input" required autoFocus />
        </Campo>
        {error && <p role="alert" className="text-sm text-rose-600">{error}</p>}
      </form>
    </Modal>
  );
}

export default function PanelPrioridades({ prioridades, onGuardado }) {
  const [edicion, setEdicion] = useState(null);

  const guardado = (mensaje) => {
    setEdicion(null);
    onGuardado(mensaje);
  };

  return (
    <Card className="overflow-hidden">
      <CabeceraPanel
        titulo="Prioridades y SLA"
        nota="El tiempo de SLA de cada prioridad se aplica a los tickets que se registren a partir del cambio; los existentes conservan el suyo."
      />
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
            <tr>
              <th scope="col" className="px-5 py-3 font-medium">Prioridad</th>
              <th scope="col" className="px-5 py-3 font-medium">SLA</th>
              <th scope="col" className="px-5 py-3"><span className="sr-only">Acciones</span></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {prioridades.map((p) => (
              <tr key={p.id}>
                <td className="px-5 py-3"><PrioridadBadge prioridad={p.nombre} /></td>
                <td className="px-5 py-3 font-medium text-slate-900">{p.slaHoras} h</td>
                <td className="px-5 py-3 text-right">
                  <Button size="sm" variant="ghost" onClick={() => setEdicion(p)} aria-label={`Editar SLA de ${p.nombre}`}>
                    <Pencil size={14} /> Editar
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {edicion && (
        <FormularioPrioridad key={edicion.id} prioridad={edicion} onCerrar={() => setEdicion(null)} onGuardado={guardado} />
      )}
    </Card>
  );
}
