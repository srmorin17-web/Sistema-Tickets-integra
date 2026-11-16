import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { ArrowRight, Lock, Mail, Ticket } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { rutaInicio } from '../lib/roles';
import { Button } from '../components/ui';

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [correo, setCorreo] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  if (user) return <Navigate to={user.debeCambiar ? '/cambiar-contrasena' : rutaInicio(user.grupo)} replace />;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setEnviando(true);
    try {
      const usuario = await login(correo.trim(), contrasena);
      navigate(usuario.debeCambiar ? '/cambiar-contrasena' : rutaInicio(usuario.grupo), { replace: true });
    } catch (err) {
      setError(err.message);
      setEnviando(false);
    }
  };

  return (
    <div className="flex min-h-screen">
      <div className="relative hidden w-1/2 flex-col justify-center bg-gradient-to-br from-cyan-700 via-cyan-600 to-cyan-500 p-12 lg:flex">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/15 shadow-lg backdrop-blur">
            <Ticket size={22} className="text-white" />
          </div>
          <div>
            <p className="text-xl font-bold tracking-tight text-white">CALMA</p>
            <p className="text-xs font-medium uppercase tracking-wider text-cyan-100">Mesa de Ayuda</p>
          </div>
        </div>
        <div className="mt-16 max-w-md">
          <h1 className="text-4xl font-bold leading-tight tracking-tight text-white">Sistema de tickets</h1>
          <p className="mt-4 text-base text-cyan-50">Fundación CALMA, Área de Tecnología.</p>
        </div>
      </div>

      <div className="flex w-full flex-col justify-center bg-slate-50 px-6 py-12 lg:w-1/2 lg:px-20">
        <div className="mx-auto w-full max-w-sm">
          <div className="mb-8 lg:hidden">
            <div className="flex items-center gap-2.5">
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-sky-600 text-white">
                <Ticket size={20} />
              </div>
              <p className="text-lg font-bold tracking-tight text-slate-900">CALMA</p>
            </div>
          </div>

          <h2 className="text-2xl font-semibold tracking-tight text-slate-900">Iniciar sesión</h2>
          <p className="mt-1 text-sm text-slate-500">Acceda con su correo institucional.</p>
          {location.state?.mensaje && (
            <p role="status" className="mt-4 rounded-lg bg-emerald-50 px-3 py-2.5 text-sm text-emerald-700 ring-1 ring-emerald-200">
              {location.state.mensaje}
            </p>
          )}

          <form onSubmit={handleSubmit} className="mt-8 space-y-4">
            <div>
              <label htmlFor="correo" className="mb-1.5 block text-sm font-medium text-slate-700">
                Correo institucional
              </label>
              <div className="relative">
                <Mail size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  id="correo"
                  type="email"
                  autoComplete="username"
                  value={correo}
                  onChange={(e) => setCorreo(e.target.value)}
                  className="input pl-10"
                  placeholder="usuario@fundacioncalma.com"
                  required
                />
              </div>
            </div>
            <div>
              <label htmlFor="contrasena" className="mb-1.5 block text-sm font-medium text-slate-700">
                Contraseña
              </label>
              <div className="relative">
                <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  id="contrasena"
                  type="password"
                  autoComplete="current-password"
                  value={contrasena}
                  onChange={(e) => setContrasena(e.target.value)}
                  className="input pl-10"
                  placeholder="••••••••"
                  required
                />
              </div>
            </div>

            {error && (
              <div role="alert" className="rounded-lg bg-rose-50 px-3 py-2.5 text-sm text-rose-700 ring-1 ring-rose-200">
                {error}
              </div>
            )}

            <Button type="submit" size="lg" className="w-full" disabled={enviando}>
              {enviando ? 'Ingresando...' : 'Ingresar'}
              {!enviando && <ArrowRight size={16} />}
            </Button>
          </form>

          {location.state?.desde && (
            <p className="mt-6 text-center text-xs text-slate-500">Inicie sesión para continuar.</p>
          )}
        </div>
      </div>
    </div>
  );
}
