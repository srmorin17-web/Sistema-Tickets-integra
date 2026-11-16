import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { CheckCircle2, Circle, KeyRound } from 'lucide-react';
import { cambiarContrasena } from '../api/usuariosApi';
import { Button } from '../components/ui';
import { useAuth } from '../context/AuthContext';
import { esValida, requisitos } from '../lib/contrasena';
import { rutaInicio } from '../lib/roles';

function mensajeDeError(error) {
  if (!error.response) return 'No hay conexión con el servidor.';
  switch (error.response.status) {
    case 403:
      return 'La contraseña actual no es correcta.';
    case 422:
      return 'La contraseña nueva no cumple los requisitos.';
    case 429:
      return 'Demasiados intentos fallidos. Espere unos minutos e intente nuevamente.';
    default:
      return 'No se pudo cambiar la contraseña. Intente nuevamente.';
  }
}

/** Cambio de contraseña: obligatorio con una contraseña temporal, voluntario desde el menú de usuario. */
export default function CambiarContrasena() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [actual, setActual] = useState('');
  const [nueva, setNueva] = useState('');
  const [repetida, setRepetida] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');

  // El correo no se guarda en la sesión; basta con el nombre de usuario que el backend comprueba
  const lista = requisitos(nueva, actual, '');
  const coinciden = nueva === repetida;
  const puedeEnviar = actual && esValida(nueva, actual, '') && coinciden && !enviando;

  const enviar = async (e) => {
    e.preventDefault();
    if (!puedeEnviar) return;
    setEnviando(true);
    setError('');
    try {
      await cambiarContrasena({ contrasenaActual: actual, contrasenaNueva: nueva });
      await logout();
      navigate('/login', {
        replace: true,
        state: { mensaje: 'La contraseña se cambió correctamente. Inicie sesión nuevamente.' },
      });
    } catch (err) {
      setError(mensajeDeError(err));
      setEnviando(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4 py-10">
      <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-7 shadow-sm">
        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-sky-50 text-sky-600">
          <KeyRound size={22} />
        </div>
        <h1 className="mt-4 text-xl font-semibold tracking-tight text-slate-900">
          {user.debeCambiar ? 'Defina su contraseña' : 'Cambiar contraseña'}
        </h1>
        <p className="mt-1 text-sm text-slate-500">
          {user.debeCambiar
            ? 'Está usando una contraseña temporal. Para continuar, elija una contraseña personal.'
            : 'Elija una contraseña nueva para su cuenta.'}
        </p>

        <form onSubmit={enviar} className="mt-6 space-y-4" noValidate>
          <div>
            <label htmlFor="actual" className="mb-1.5 block text-sm font-medium text-slate-700">
              {user.debeCambiar ? 'Contraseña temporal' : 'Contraseña actual'}
            </label>
            <input id="actual" type="password" autoComplete="current-password" value={actual} onChange={(e) => setActual(e.target.value)} className="input" required />
          </div>
          <div>
            <label htmlFor="nueva" className="mb-1.5 block text-sm font-medium text-slate-700">Contraseña nueva</label>
            <input id="nueva" type="password" autoComplete="new-password" value={nueva} onChange={(e) => setNueva(e.target.value)} className="input" required aria-describedby="requisitos" />
            <ul id="requisitos" className="mt-2 space-y-1">
              {lista.map((r) => (
                <li key={r.id} className={`flex items-center gap-1.5 text-xs ${r.cumple ? 'text-emerald-700' : 'text-slate-500'}`}>
                  {r.cumple ? <CheckCircle2 size={13} /> : <Circle size={13} />} {r.texto}
                </li>
              ))}
            </ul>
          </div>
          <div>
            <label htmlFor="repetida" className="mb-1.5 block text-sm font-medium text-slate-700">Repita la contraseña nueva</label>
            <input id="repetida" type="password" autoComplete="new-password" value={repetida} onChange={(e) => setRepetida(e.target.value)} className="input" required />
            {repetida && !coinciden && <p className="mt-1 text-xs text-rose-600">Las contraseñas no coinciden.</p>}
          </div>

          {error && <p role="alert" className="rounded-lg bg-rose-50 px-3 py-2.5 text-sm text-rose-700 ring-1 ring-rose-200">{error}</p>}

          <Button type="submit" size="lg" className="w-full" disabled={!puedeEnviar}>
            {enviando ? 'Guardando...' : 'Guardar contraseña'}
          </Button>
        </form>

        <div className="mt-5 text-center text-sm">
          {user.debeCambiar ? (
            <button type="button" onClick={() => { logout(); navigate('/login', { replace: true }); }} className="text-slate-500 underline hover:text-slate-700">
              Cerrar sesión
            </button>
          ) : (
            <Link to={rutaInicio(user.grupo)} className="text-slate-500 underline hover:text-slate-700">Volver</Link>
          )}
        </div>
      </div>
    </div>
  );
}
