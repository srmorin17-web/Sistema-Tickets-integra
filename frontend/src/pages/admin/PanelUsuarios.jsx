import { useEffect, useMemo, useRef, useState } from 'react';
import { Check, Copy, KeyRound, Mail, Pencil, Plus, Power, Search, ShieldAlert, UserPlus } from 'lucide-react';
import { actualizarProvisionamiento, actualizarUsuario, crearUsuario, encolarProvisionamientoMasivo, listarRoles, listarUsuarios, mensajeDeUsuarios, obtenerProvisionamiento, reenviarActivacion, restablecerContrasena } from '../../api/usuariosApi';
import { Modal } from '../../components/Overlay';
import SelectorEmpleado from '../../components/SelectorEmpleado';
import { Badge, Button, Card, EmptyState, Spinner } from '../../components/ui';
import { timeAgo } from '../../lib/ui';
import { CabeceraPanel, Campo, EstadoActivo } from './comunes';

function FormularioNuevo({ roles, onCerrar, onCreado }) {
  const [empleado, setEmpleado] = useState(null);
  const [rolId, setRolId] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');
  const asignables = roles.filter((r) => r.asignable);

  const guardar = async (e) => {
    e.preventDefault();
    if (!empleado || !rolId || enviando) return;
    setEnviando(true);
    setError('');
    try {
      onCreado(await crearUsuario({ empleadoId: empleado.id, rolId }));
    } catch (err) {
      setError(mensajeDeUsuarios(err));
      setEnviando(false);
    }
  };

  return (
    <Modal
      onClose={enviando ? () => {} : onCerrar}
      title="Nuevo usuario"
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onCerrar} disabled={enviando}>Cancelar</Button>
          <Button type="submit" form="form-usuario-nuevo" disabled={!empleado || !rolId || enviando}>
            {enviando ? 'Creando...' : 'Crear cuenta'}
          </Button>
        </div>
      }
    >
      <form id="form-usuario-nuevo" onSubmit={guardar} className="space-y-4">
        <Campo id="usuario-empleado" etiqueta="Empleado" ayuda="Solo empleados activos de RR. HH. que aún no tienen cuenta.">
          <SelectorEmpleado id="usuario-empleado" seleccionado={empleado} onChange={setEmpleado} />
        </Campo>
        <Campo id="usuario-rol" etiqueta="Rol" ayuda="Los roles disponibles dependen de su propio rol.">
          <select id="usuario-rol" value={rolId} onChange={(e) => setRolId(e.target.value)} className="input" required>
            <option value="">Seleccione un rol...</option>
            {asignables.map((r) => (
              <option key={r.id} value={r.id}>{r.nombre}</option>
            ))}
          </select>
        </Campo>
        <p className="text-xs text-slate-500">
          La cuenta recibirá un enlace de activación en su correo corporativo para que pueda definir su contraseña.
        </p>
        {error && <p role="alert" className="text-sm text-rose-600">{error}</p>}
      </form>
    </Modal>
  );
}

function FormularioRol({ cuenta, roles, onCerrar, onGuardado }) {
  const [rolId, setRolId] = useState(cuenta.rolId);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');
  const asignables = roles.filter((r) => r.asignable || r.id === cuenta.rolId);

  const guardar = async (e) => {
    e.preventDefault();
    if (rolId === cuenta.rolId || enviando) return;
    setEnviando(true);
    setError('');
    try {
      await actualizarUsuario(cuenta.id, { rolId });
      onGuardado(`Rol de ${cuenta.nombre} actualizado.`);
    } catch (err) {
      setError(mensajeDeUsuarios(err));
      setEnviando(false);
    }
  };

  return (
    <Modal
      onClose={enviando ? () => {} : onCerrar}
      title="Cambiar rol"
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onCerrar} disabled={enviando}>Cancelar</Button>
          <Button type="submit" form="form-usuario-rol" disabled={rolId === cuenta.rolId || enviando}>
            {enviando ? 'Guardando...' : 'Guardar'}
          </Button>
        </div>
      }
    >
      <form id="form-usuario-rol" onSubmit={guardar} className="space-y-4">
        <p className="text-sm text-slate-600">{cuenta.nombre} <span className="text-slate-400">({cuenta.correo})</span></p>
        <Campo id="cambio-rol" etiqueta="Rol" ayuda="El cambio se aplica de inmediato, incluso si la persona tiene la sesión abierta.">
          <select id="cambio-rol" value={rolId} onChange={(e) => setRolId(e.target.value)} className="input" autoFocus>
            {asignables.map((r) => (
              <option key={r.id} value={r.id}>{r.nombre}</option>
            ))}
          </select>
        </Campo>
        {error && <p role="alert" className="text-sm text-rose-600">{error}</p>}
      </form>
    </Modal>
  );
}

function Confirmacion({ titulo, texto, textoBoton, peligro = false, onConfirmar, onCerrar }) {
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');

  const confirmar = async () => {
    setEnviando(true);
    setError('');
    try {
      await onConfirmar();
    } catch (err) {
      setError(mensajeDeUsuarios(err));
      setEnviando(false);
    }
  };

  return (
    <Modal
      onClose={enviando ? () => {} : onCerrar}
      title={titulo}
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onCerrar} disabled={enviando}>Cancelar</Button>
          <Button variant={peligro ? 'danger' : 'primary'} onClick={confirmar} disabled={enviando}>
            {enviando ? 'Procesando...' : textoBoton}
          </Button>
        </div>
      }
    >
      <p className="text-sm text-slate-600">{texto}</p>
      {error && <p role="alert" className="mt-3 text-sm text-rose-600">{error}</p>}
    </Modal>
  );
}

/** Muestra la contraseña temporal UNA sola vez: no se guarda en claro en ningún lugar. */
function ContrasenaTemporalModal({ datos, onCerrar }) {
  const [copiada, setCopiada] = useState(false);

  const copiar = async () => {
    try {
      await navigator.clipboard.writeText(datos.contrasena);
      setCopiada(true);
    } catch {
      setCopiada(false); // sin permiso del portapapeles: la persona puede copiarla a mano
    }
  };

  return (
    <Modal
      onClose={() => {}}
      title={datos.titulo}
      footer={<div className="flex justify-end"><Button onClick={onCerrar}>Ya la anoté, cerrar</Button></div>}
    >
      <div className="space-y-4">
        <p className="text-sm text-slate-600">
          Entregue esta contraseña temporal a <strong>{datos.nombre}</strong> ({datos.correo}) por un medio seguro.
          La persona deberá cambiarla al iniciar sesión.
        </p>
        <div className="flex items-center justify-between gap-3 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
          <code className="select-all font-mono text-lg font-semibold tracking-wider text-slate-900" aria-label="Contraseña temporal">
            {datos.contrasena}
          </code>
          <Button size="sm" variant="outline" onClick={copiar}>
            {copiada ? <Check size={14} /> : <Copy size={14} />} {copiada ? 'Copiada' : 'Copiar'}
          </Button>
        </div>
        <p role="note" className="flex items-start gap-2 rounded-lg bg-amber-50 px-3 py-2.5 text-sm text-amber-800 ring-1 ring-amber-200">
          <ShieldAlert size={15} className="mt-0.5 shrink-0" />
          Se muestra una sola vez y no se puede volver a consultar. Si se pierde, restablezca la contraseña de la cuenta.
        </p>
      </div>
    </Modal>
  );
}

export default function PanelUsuarios() {
  const [resultado, setResultado] = useState({ clave: null, cuentas: [], roles: [], error: '' });
  const [recarga, setRecarga] = useState(0);
  const [busqueda, setBusqueda] = useState('');
  const [modal, setModal] = useState(null);
  const [temporal, setTemporal] = useState(null);
  const [aviso, setAviso] = useState('');
  const [errorAccion, setErrorAccion] = useState('');
  const [provisionamiento, setProvisionamiento] = useState(null);
  const [actualizandoProvisionamiento, setActualizandoProvisionamiento] = useState(false);
  const colaEstuvoActiva = useRef(false);

  const clave = String(recarga);
  const colaEnProceso = Boolean(provisionamiento && provisionamiento.pendientes + provisionamiento.procesando > 0);

  useEffect(() => {
    let activo = true;
    Promise.all([listarUsuarios(), listarRoles()])
      .then(([cuentas, roles]) => activo && setResultado({ clave, cuentas, roles, error: '' }))
      .catch((e) => activo && setResultado({ clave, cuentas: [], roles: [], error: mensajeDeUsuarios(e, 'No se pudo cargar la lista de usuarios.') }));
    return () => {
      activo = false;
    };
  }, [clave]);

  useEffect(() => {
    let activo = true;
    obtenerProvisionamiento()
      .then((estado) => activo && setProvisionamiento(estado))
      .catch((e) => activo && setErrorAccion(mensajeDeUsuarios(e, 'No se pudo consultar el aprovisionamiento de cuentas.')));
    return () => {
      activo = false;
    };
  }, []);

  useEffect(() => {
    if (!colaEnProceso) return undefined;
    let activo = true;
    const timer = window.setInterval(() => {
      obtenerProvisionamiento()
        .then((estado) => activo && setProvisionamiento(estado))
        .catch((e) => activo && setErrorAccion(mensajeDeUsuarios(e, 'No se pudo actualizar el estado del aprovisionamiento.')));
    }, 2500);
    return () => {
      activo = false;
      window.clearInterval(timer);
    };
  }, [colaEnProceso]);

  useEffect(() => {
    if (colaEnProceso) {
      colaEstuvoActiva.current = true;
    } else if (colaEstuvoActiva.current) {
      colaEstuvoActiva.current = false;
      setRecarga((n) => n + 1);
    }
  }, [colaEnProceso]);

  const cargando = resultado.clave !== clave;
  const { cuentas, roles, error } = resultado;

  const filtradas = useMemo(() => {
    const q = busqueda.trim().toLowerCase();
    return cuentas.filter((c) => !q || [c.nombre, c.correo, c.rol].some((x) => x.toLowerCase().includes(q)));
  }, [cuentas, busqueda]);

  const terminar = (mensaje) => {
    setModal(null);
    setAviso(mensaje);
    setRecarga((n) => n + 1);
  };

  return (
    <Card className="overflow-hidden">
      <CabeceraPanel
        titulo="Usuarios"
        nota="Cuentas con acceso al sistema. Se crean a partir de empleados activos de RR. HH. Los cambios de rol y las desactivaciones se aplican de inmediato."
        accion={<Button size="sm" onClick={() => { setAviso(''); setModal({ tipo: 'nuevo' }); }}><Plus size={14} /> Nuevo usuario</Button>}
      />

      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 bg-slate-50 px-5 py-3">
        <div className="text-sm">
          <p className="font-medium text-slate-800">
            Aprovisionamiento automático: {provisionamiento ? (provisionamiento.automatico ? 'Habilitado' : 'Deshabilitado') : 'Consultando...'}
          </p>
          {provisionamiento && (
            <p className="text-xs text-slate-500">
              Elegibles: {provisionamiento.elegibles} · En cola: {provisionamiento.pendientes} · Procesando: {provisionamiento.procesando} · Completadas: {provisionamiento.completados} · Fallidas: {provisionamiento.fallidos}
            </p>
          )}
        </div>
        <div className="flex flex-wrap gap-2">
          <Button
            size="sm"
            variant="outline"
            disabled={!provisionamiento || actualizandoProvisionamiento}
            onClick={async () => {
              setActualizandoProvisionamiento(true);
              setErrorAccion('');
              try {
                setProvisionamiento(await actualizarProvisionamiento(!provisionamiento.automatico));
                setAviso(!provisionamiento.automatico
                  ? 'Las próximas sincronizaciones de RR. HH. aprovisionarán cuentas de rol Empleado.'
                  : 'Se deshabilitó el aprovisionamiento automático. La cola actual seguirá procesándose.');
              } catch (e) {
                setErrorAccion(mensajeDeUsuarios(e, 'No se pudo cambiar el aprovisionamiento automático.'));
              } finally {
                setActualizandoProvisionamiento(false);
              }
            }}
          >
            {provisionamiento?.automatico ? 'Deshabilitar automático' : 'Habilitar automático'}
          </Button>
          <Button
            size="sm"
            disabled={!provisionamiento || actualizandoProvisionamiento}
            onClick={() => { setAviso(''); setModal({ tipo: 'aprovisionamiento' }); }}
          >
            <UserPlus size={14} /> Crear/reintentar cuentas
          </Button>
        </div>
      </div>

      <div className="border-b border-slate-200 px-5 py-3">
        <div className="relative max-w-sm">
          <label htmlFor="busqueda-usuarios" className="sr-only">Buscar usuario</label>
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input id="busqueda-usuarios" value={busqueda} onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por nombre, correo o rol" className="input py-2 pl-10" />
        </div>
      </div>

      {aviso && <p role="status" className="border-b border-emerald-200 bg-emerald-50 px-5 py-2.5 text-sm text-emerald-700">{aviso}</p>}
      {errorAccion && <p role="alert" className="border-b border-rose-200 bg-rose-50 px-5 py-2.5 text-sm text-rose-700">{errorAccion}</p>}
      {error && <p role="alert" className="border-b border-rose-200 bg-rose-50 px-5 py-2.5 text-sm text-rose-700">{error}</p>}

      {cargando && cuentas.length === 0 ? (
        <Spinner label="Cargando usuarios..." />
      ) : filtradas.length === 0 ? (
        <EmptyState icon={<Search size={20} />} title="Ningún usuario coincide" desc="Pruebe con otra búsqueda." />
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
              <tr>
                <th scope="col" className="px-5 py-3 font-medium">Usuario</th>
                <th scope="col" className="px-5 py-3 font-medium">Rol</th>
                <th scope="col" className="px-5 py-3 font-medium">Estado</th>
                <th scope="col" className="px-5 py-3 font-medium">Último acceso</th>
                <th scope="col" className="px-5 py-3"><span className="sr-only">Acciones</span></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filtradas.map((c) => (
                <tr key={c.id} className={c.activo ? '' : 'text-slate-400'}>
                  <td className="px-5 py-3">
                    <p className={`font-medium ${c.activo ? 'text-slate-900' : 'text-slate-400'}`}>
                      {c.nombre}{c.esActual && <span className="ml-1.5 text-xs font-normal text-slate-500">(usted)</span>}
                    </p>
                    <p className="text-xs text-slate-500">{c.correo}</p>
                  </td>
                  <td className="px-5 py-3"><Badge className="bg-slate-100 text-slate-700 ring-slate-200">{c.rol}</Badge></td>
                  <td className="px-5 py-3">
                    <div className="flex flex-wrap items-center gap-1.5">
                      <EstadoActivo activo={c.activo} />
                      {!c.activada && <Badge className="bg-amber-50 text-amber-700 ring-amber-200">Pendiente de activación</Badge>}
                      {c.debeCambiarContrasena && <Badge className="bg-amber-50 text-amber-700 ring-amber-200">Contraseña temporal</Badge>}
                    </div>
                  </td>
                  <td className="px-5 py-3 text-xs text-slate-500">{c.ultimoAcceso ? timeAgo(c.ultimoAcceso) : 'Nunca'}</td>
                  <td className="px-5 py-3">
                    {c.puedeGestionar && (
                      <div className="flex justify-end gap-1">
                        {!c.activada && (
                          <Button size="sm" variant="ghost" onClick={async () => {
                            try {
                              await reenviarActivacion(c.id);
                              setErrorAccion('');
                              setAviso(`Enlace de activación reenviado a ${c.correo}.`);
                            } catch (err) {
                              setAviso('');
                              setErrorAccion(err.response?.status === 409
                                ? 'La cuenta ya fue activada.'
                                : mensajeDeUsuarios(err, 'No se pudo reenviar el enlace de activación.'));
                            }
                          }} aria-label={`Reenviar activación a ${c.nombre}`}>
                            <Mail size={14} /> Reenviar enlace
                          </Button>
                        )}
                        <Button size="sm" variant="ghost" onClick={() => { setAviso(''); setModal({ tipo: 'rol', cuenta: c }); }} aria-label={`Cambiar rol de ${c.nombre}`}>
                          <Pencil size={14} /> Rol
                        </Button>
                        {c.activada && (
                          <Button size="sm" variant="ghost" onClick={() => { setAviso(''); setModal({ tipo: 'restablecer', cuenta: c }); }} aria-label={`Restablecer contraseña de ${c.nombre}`}>
                            <KeyRound size={14} /> Contraseña
                          </Button>
                        )}
                        <Button size="sm" variant="ghost" onClick={() => { setAviso(''); setModal({ tipo: 'estado', cuenta: c }); }}
                          aria-label={`${c.activo ? 'Desactivar' : 'Activar'} a ${c.nombre}`}>
                          <Power size={14} /> {c.activo ? 'Desactivar' : 'Activar'}
                        </Button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {modal?.tipo === 'nuevo' && (
        <FormularioNuevo roles={roles} onCerrar={() => setModal(null)}
          onCreado={(r) => {
            setModal(null);
            setErrorAccion('');
            setAviso(`Cuenta creada. Enviamos un enlace de activación a ${r.cuenta.correo}.`);
            setRecarga((n) => n + 1);
          }} />
      )}
      {modal?.tipo === 'rol' && (
        <FormularioRol key={modal.cuenta.id} cuenta={modal.cuenta} roles={roles} onCerrar={() => setModal(null)} onGuardado={terminar} />
      )}
      {modal?.tipo === 'estado' && (
        <Confirmacion
          titulo={modal.cuenta.activo ? 'Desactivar cuenta' : 'Activar cuenta'}
          texto={modal.cuenta.activo
            ? `${modal.cuenta.nombre} dejará de poder iniciar sesión y perderá el acceso de inmediato, aunque tenga la sesión abierta. Sus tickets e historial se conservan.`
            : `${modal.cuenta.nombre} podrá volver a iniciar sesión con su contraseña actual.`}
          textoBoton={modal.cuenta.activo ? 'Desactivar' : 'Activar'}
          peligro={modal.cuenta.activo}
          onCerrar={() => setModal(null)}
          onConfirmar={async () => {
            await actualizarUsuario(modal.cuenta.id, { activo: !modal.cuenta.activo });
            terminar(`Cuenta de ${modal.cuenta.nombre} ${modal.cuenta.activo ? 'desactivada' : 'activada'}.`);
          }}
        />
      )}
      {modal?.tipo === 'aprovisionamiento' && (
        <Confirmacion
          titulo="Aprovisionar cuentas pendientes"
          texto={`Se pondrán en cola hasta ${provisionamiento?.elegibles ?? 0} empleados activos sincronizados desde RR. HH. que todavía no tengan cuenta, además de reintentar las solicitudes fallidas. Se asignará el rol Empleado y se enviará a cada correo corporativo un enlace para establecer su contraseña. El proceso continuará en segundo plano.`}
          textoBoton="Crear cuentas"
          onCerrar={() => setModal(null)}
          onConfirmar={async () => {
            const resultadoCola = await encolarProvisionamientoMasivo();
            const estadoNuevo = await obtenerProvisionamiento();
            setProvisionamiento(estadoNuevo);
            setRecarga((n) => n + 1);
            setModal(null);
            setErrorAccion('');
            setAviso(resultadoCola.encolados > 0
              ? `Se encolaron ${resultadoCola.encolados} cuentas. El procesamiento se realizará en segundo plano.`
              : 'No hay empleados activos sincronizados que necesiten una cuenta.');
          }}
        />
      )}
      {modal?.tipo === 'restablecer' && (
        <Confirmacion
          titulo="Restablecer contraseña"
          texto={`Se generará una contraseña temporal para ${modal.cuenta.nombre}. La contraseña actual dejará de servir y deberá cambiar la nueva al ingresar.`}
          textoBoton="Restablecer"
          onCerrar={() => setModal(null)}
          onConfirmar={async () => {
            const r = await restablecerContrasena(modal.cuenta.id);
            setModal(null);
            setTemporal({ titulo: 'Contraseña restablecida', nombre: modal.cuenta.nombre, correo: modal.cuenta.correo, contrasena: r.contrasenaTemporal });
            setRecarga((n) => n + 1);
          }}
        />
      )}
      {temporal && <ContrasenaTemporalModal datos={temporal} onCerrar={() => setTemporal(null)} />}
    </Card>
  );
}
