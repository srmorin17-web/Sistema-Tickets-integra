import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { CheckCircle2, Circle, KeyRound } from 'lucide-react';
import { activarCuenta } from '../api/activacionApi';
import { Button } from '../components/ui';
import { esValida, requisitos } from '../lib/contrasena';

export default function ActivarCuenta() {
  const [searchParams] = useSearchParams();
  const [contrasena, setContrasena] = useState('');
  const [repetida, setRepetida] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');
  const [activada, setActivada] = useState(false);
  const token = searchParams.get('token') || '';
  useEffect(() => {
    if (token) window.history.replaceState(null, '', '/activar-cuenta');
  }, [token]);
  const coinciden = contrasena === repetida;
  const puedeEnviar = token && esValida(contrasena, '', '') && coinciden && !enviando;

  const enviar = async (e) => {
    e.preventDefault();
    if (!puedeEnviar) return;
    setEnviando(true);
    setError('');
    try {
      await activarCuenta({ token, contrasenaNueva: contrasena });
      setActivada(true);
    } catch (err) {
      setError(err.response?.status === 422
        ? 'La contraseña no cumple los requisitos. Revísalos e inténtalo nuevamente.'
        : err.response?.status === 400
          ? 'El enlace no es válido o ya venció. Solicita al administrador que reenvíe la activación.'
          : 'No se pudo activar la cuenta. Inténtalo nuevamente.');
      setEnviando(false);
    }
  };

  const lista = requisitos(contrasena, '', '');

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4 py-10">
      <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-7 shadow-sm">
        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-sky-50 text-sky-600">
          <KeyRound size={22} />
        </div>
        <h1 className="mt-4 text-xl font-semibold tracking-tight text-slate-900">
          {activada ? 'Cuenta activada' : 'Active su cuenta'}
        </h1>
        {activada ? (
          <div className="mt-4 space-y-4">
            <p role="status" className="text-sm text-emerald-700">
              Su contraseña se guardó correctamente. Ya puede iniciar sesión.
            </p>
            <Link to="/login" className="block">
              <Button size="lg" className="w-full">Ir al inicio de sesión</Button>
            </Link>
          </div>
        ) : !token ? (
          <p role="alert" className="mt-4 text-sm text-rose-700">
            Falta el token de activación. Abra el enlace completo recibido por correo.
          </p>
        ) : (
          <>
            <p className="mt-1 text-sm text-slate-500">
              Elija una contraseña personal para completar la activación.
            </p>
            <form onSubmit={enviar} className="mt-6 space-y-4" noValidate>
              <div>
                <label htmlFor="contrasena" className="mb-1.5 block text-sm font-medium text-slate-700">
                  Nueva contraseña
                </label>
                <input id="contrasena" type="password" autoComplete="new-password" value={contrasena}
                  onChange={(e) => setContrasena(e.target.value)} className="input" required aria-describedby="requisitos-activacion" />
                <ul id="requisitos-activacion" className="mt-2 space-y-1">
                  {lista.map((r) => (
                    <li key={r.id} className={`flex items-center gap-1.5 text-xs ${r.cumple ? 'text-emerald-700' : 'text-slate-500'}`}>
                      {r.cumple ? <CheckCircle2 size={13} /> : <Circle size={13} />} {r.texto}
                    </li>
                  ))}
                </ul>
              </div>
              <div>
                <label htmlFor="repetida" className="mb-1.5 block text-sm font-medium text-slate-700">
                  Repita la contraseña
                </label>
                <input id="repetida" type="password" autoComplete="new-password" value={repetida}
                  onChange={(e) => setRepetida(e.target.value)} className="input" required />
                {repetida && !coinciden && (
                  <p className="mt-1 text-xs text-rose-600">Las contraseñas no coinciden.</p>
                )}
              </div>
              {error && <p role="alert" className="rounded-lg bg-rose-50 px-3 py-2.5 text-sm text-rose-700 ring-1 ring-rose-200">{error}</p>}
              <Button type="submit" size="lg" className="w-full" disabled={!puedeEnviar}>
                {enviando ? 'Activando...' : 'Guardar contraseña y activar'}
              </Button>
            </form>
          </>
        )}
      </div>
    </div>
  );
}
