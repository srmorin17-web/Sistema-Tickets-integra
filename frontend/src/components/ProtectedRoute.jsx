import { useEffect, useRef } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import { reportarVistaDenegada } from '../api/seguridadApi';
import { useAuth } from '../context/AuthContext';
import { Card, EmptyState } from './ui';

/** Pantalla de acceso denegado. Además deja registrado el intento en el backend (tabla intento_acceso). */
function AccesoDenegado({ ruta }) {
  const reportado = useRef(false);

  useEffect(() => {
    if (reportado.current) return;
    reportado.current = true;
    reportarVistaDenegada(ruta).catch(() => {}); // el aviso no debe afectar la pantalla
  }, [ruta]);

  return (
    <Card>
      <EmptyState
        icon={<ShieldAlert size={22} />}
        title="Permisos insuficientes"
        desc="Su rol no tiene acceso a esta sección. El intento quedó registrado."
      />
    </Card>
  );
}

/**
 * Exige sesión activa. Opcionalmente restringe el acceso por perfil (`grupos`: solicitante, tecnico,
 * supervisor, admin) o por rol exacto del backend (`roles`: p. ej. ['Superadmin', 'Director']).
 */
export default function ProtectedRoute({ children, grupos, roles }) {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) return <Navigate to="/login" replace state={{ desde: location.pathname }} />;
  // Con una contraseña temporal solo se permite la pantalla de cambio (el backend lo exige también)
  if (user.debeCambiar && location.pathname !== '/cambiar-contrasena') return <Navigate to="/cambiar-contrasena" replace />;

  const sinPerfil = grupos && !grupos.includes(user.grupo);
  const sinRol = roles && !roles.includes(user.rol);
  if (sinPerfil || sinRol) return <AccesoDenegado ruta={location.pathname} />;

  return children;
}
