import { useEffect, useState } from 'react';
import { Lock, RefreshCw } from 'lucide-react';
import { obtenerAccesos, obtenerGestionUsuarios, obtenerIntentos } from '../api/seguridadApi';
import { mensajeDeError } from '../api/ticketsApi';
import { Badge, Button, Card, EmptyState, Spinner } from '../components/ui';
import { formatDate } from '../lib/ui';

const PESTANAS = [
  { id: 'accesos', etiqueta: 'Inicios de sesión' },
  { id: 'intentos', etiqueta: 'Accesos denegados' },
  { id: 'usuarios', etiqueta: 'Gestión de usuarios' },
];

const LIMITE = 200;

const ACCION_USUARIO = {
  CREAR_USUARIO: 'Cuenta creada',
  CAMBIO_ROL: 'Cambio de rol',
  ACTIVAR: 'Cuenta activada',
  DESACTIVAR: 'Cuenta desactivada',
  RESTABLECER_CONTRASENA: 'Contraseña restablecida',
  CAMBIO_CONTRASENA: 'Contraseña cambiada',
};

const RESULTADO_ACCESO = {
  EXITOSO: { texto: 'Exitoso', clase: 'bg-emerald-50 text-emerald-700 ring-emerald-200' },
  FALLIDO: { texto: 'Fallido', clase: 'bg-rose-50 text-rose-700 ring-rose-200' },
  BLOQUEADO: { texto: 'Bloqueado', clase: 'bg-amber-50 text-amber-700 ring-amber-200' },
};

const TIPO_INTENTO = { DENEGADO: 'Endpoint de la API', VISTA_DENEGADA: 'Pantalla' };

function TablaAccesos({ filas }) {
  return (
    <table className="w-full text-left text-sm">
      <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
        <tr>
          <th scope="col" className="px-5 py-3 font-medium">Fecha y hora</th>
          <th scope="col" className="px-5 py-3 font-medium">Correo</th>
          <th scope="col" className="px-5 py-3 font-medium">Resultado</th>
          <th scope="col" className="px-5 py-3 font-medium">IP</th>
        </tr>
      </thead>
      <tbody className="divide-y divide-slate-100">
        {filas.map((f) => (
          <tr key={f.id}>
            <td className="whitespace-nowrap px-5 py-2.5 text-slate-600">{formatDate(f.fecha)}</td>
            <td className="px-5 py-2.5 font-medium text-slate-900">{f.correo || '(vacío)'}</td>
            <td className="px-5 py-2.5">
              <Badge className={(RESULTADO_ACCESO[f.resultado] ?? RESULTADO_ACCESO.FALLIDO).clase}>
                {(RESULTADO_ACCESO[f.resultado] ?? { texto: f.resultado }).texto}
              </Badge>
            </td>
            <td className="px-5 py-2.5 font-mono text-xs text-slate-600">{f.ip ?? '—'}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function TablaIntentos({ filas }) {
  return (
    <table className="w-full text-left text-sm">
      <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
        <tr>
          <th scope="col" className="px-5 py-3 font-medium">Fecha y hora</th>
          <th scope="col" className="px-5 py-3 font-medium">Usuario</th>
          <th scope="col" className="px-5 py-3 font-medium">Ruta</th>
          <th scope="col" className="px-5 py-3 font-medium">Tipo</th>
          <th scope="col" className="px-5 py-3 font-medium">IP</th>
        </tr>
      </thead>
      <tbody className="divide-y divide-slate-100">
        {filas.map((f) => (
          <tr key={f.id}>
            <td className="whitespace-nowrap px-5 py-2.5 text-slate-600">{formatDate(f.fecha)}</td>
            <td className="px-5 py-2.5 font-medium text-slate-900">{f.usuario ?? 'Sin sesión'}</td>
            <td className="px-5 py-2.5 font-mono text-xs text-slate-700">
              <span className="mr-1.5 rounded bg-slate-100 px-1.5 py-0.5 text-slate-500">{f.metodo}</span>
              {f.ruta}
            </td>
            <td className="px-5 py-2.5 text-xs text-slate-600">{TIPO_INTENTO[f.resultado] ?? f.resultado}</td>
            <td className="px-5 py-2.5 font-mono text-xs text-slate-600">{f.ip ?? '—'}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function TablaGestionUsuarios({ filas }) {
  return (
    <table className="w-full text-left text-sm">
      <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
        <tr>
          <th scope="col" className="px-5 py-3 font-medium">Fecha y hora</th>
          <th scope="col" className="px-5 py-3 font-medium">Acción</th>
          <th scope="col" className="px-5 py-3 font-medium">Cuenta afectada</th>
          <th scope="col" className="px-5 py-3 font-medium">Realizada por</th>
          <th scope="col" className="px-5 py-3 font-medium">Detalle</th>
          <th scope="col" className="px-5 py-3 font-medium">IP</th>
        </tr>
      </thead>
      <tbody className="divide-y divide-slate-100">
        {filas.map((f) => (
          <tr key={f.id}>
            <td className="whitespace-nowrap px-5 py-2.5 text-slate-600">{formatDate(f.fecha)}</td>
            <td className="px-5 py-2.5 font-medium text-slate-900">{ACCION_USUARIO[f.accion] ?? f.accion}</td>
            <td className="px-5 py-2.5 text-slate-700">{f.afectado ?? '—'}</td>
            <td className="px-5 py-2.5 text-slate-700">{f.actor === f.afectado ? 'La propia persona' : (f.actor ?? 'Sistema')}</td>
            <td className="px-5 py-2.5 text-xs text-slate-500">{f.detalle ?? ''}</td>
            <td className="px-5 py-2.5 font-mono text-xs text-slate-600">{f.ip ?? '—'}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export default function Auditoria() {
  const [pestana, setPestana] = useState('accesos');
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, filas: [], error: '' });

  const clave = `${pestana}|${recarga}`;

  useEffect(() => {
    let activo = true;
    const consulta = pestana === 'accesos' ? obtenerAccesos(LIMITE) : pestana === 'intentos' ? obtenerIntentos(LIMITE) : obtenerGestionUsuarios(LIMITE);
    consulta
      .then((filas) => activo && setResultado({ clave, filas, error: '' }))
      .catch((e) =>
        activo && setResultado({ clave, filas: [], error: mensajeDeError(e, 'No se pudo cargar el registro de auditoría.') })
      );
    return () => {
      activo = false;
    };
  }, [clave, pestana]);

  const cargando = resultado.clave !== clave;
  const { filas, error } = resultado;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Auditoría</h1>
          <p className="mt-1 flex items-center gap-1.5 text-sm text-slate-500">
            <Lock size={14} /> Registros de solo lectura: la base de datos impide modificarlos o eliminarlos. Se muestran los últimos {LIMITE}.
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => setRecarga((n) => n + 1)} disabled={cargando}>
          <RefreshCw size={14} className={cargando ? 'animate-spin' : ''} />
          {cargando ? 'Actualizando...' : 'Actualizar'}
        </Button>
      </div>

      <div className="flex gap-1 overflow-x-auto rounded-lg border border-slate-200 bg-white p-1" role="tablist" aria-label="Registros de auditoría">
        {PESTANAS.map((p) => (
          <button key={p.id} type="button" role="tab" id={`tab-${p.id}`} aria-selected={pestana === p.id} aria-controls={`panel-${p.id}`}
            onClick={() => setPestana(p.id)}
            className={`whitespace-nowrap rounded-md px-4 py-2 text-sm font-medium transition-colors ${
              pestana === p.id ? 'bg-sky-600 text-white' : 'text-slate-600 hover:bg-slate-100'
            }`}>
            {p.etiqueta}
          </button>
        ))}
      </div>

      {error && (
        <div role="alert" className="rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200">{error}</div>
      )}

      <Card className="overflow-hidden">
        <div role="tabpanel" id={`panel-${pestana}`} aria-labelledby={`tab-${pestana}`} className="overflow-x-auto">
          {cargando ? (
            <Spinner label="Cargando registros..." />
          ) : filas.length === 0 && !error ? (
            <EmptyState icon={<Lock size={22} />} title="Sin registros" desc="Aún no hay eventos de este tipo." />
          ) : pestana === 'accesos' ? (
            <TablaAccesos filas={filas} />
          ) : pestana === 'intentos' ? (
            <TablaIntentos filas={filas} />
          ) : (
            <TablaGestionUsuarios filas={filas} />
          )}
        </div>
      </Card>
    </div>
  );
}
