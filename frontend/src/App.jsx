import { Suspense, lazy } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ROLES_AUDITORIA, rutaInicio } from './lib/roles';
import AppShell from './components/AppShell';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import { Spinner } from './components/ui';
import Administracion from './pages/Administracion';
import Asignacion from './pages/Asignacion';
import Auditoria from './pages/Auditoria';
import ActivarCuenta from './pages/ActivarCuenta';
import BandejaTecnico from './pages/BandejaTecnico';
import CambiarContrasena from './pages/CambiarContrasena';
import EscaladosMiArea from './pages/EscaladosMiArea';
import MisTickets from './pages/MisTickets';
import NuevoTicket from './pages/NuevoTicket';
import Reportes from './pages/Reportes';
import TodosTickets from './pages/TodosTickets';

function Inicio() {
  const { user } = useAuth();
  return <Navigate to={user ? rutaInicio(user.grupo) : '/login'} replace />;
}

const Dashboard = lazy(() => import('./pages/Dashboard'));

const proteger = (grupos, elemento) => <ProtectedRoute grupos={grupos}>{elemento}</ProtectedRoute>;

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/activar-cuenta" element={<ActivarCuenta />} />
        <Route path="/cambiar-contrasena" element={<ProtectedRoute><CambiarContrasena /></ProtectedRoute>} />

        <Route
          element={
            <ProtectedRoute>
              <AppShell />
            </ProtectedRoute>
          }
        >
          <Route index element={<Inicio />} />

          <Route path="mis-tickets" element={proteger(['solicitante'], <MisTickets />)} />
          <Route path="nuevo-ticket" element={proteger(['solicitante'], <NuevoTicket />)} />

          <Route path="todos-tickets" element={proteger(['admin'], <TodosTickets />)} />
          <Route path="bandeja" element={proteger(['tecnico', 'supervisor'], <BandejaTecnico />)} />
          <Route path="dashboard" element={proteger(['supervisor', 'admin'],
            <Suspense fallback={<Spinner label="Cargando dashboard..." />}><Dashboard /></Suspense>)} />

          <Route path="mi-area" element={<ProtectedRoute><EscaladosMiArea /></ProtectedRoute>} />
          <Route path="asignacion" element={proteger(['supervisor', 'admin'], <Asignacion />)} />
          <Route path="reportes" element={proteger(['supervisor', 'admin'], <Reportes />)} />
          <Route path="administracion" element={proteger(['admin'], <Administracion />)} />
          <Route path="auditoria" element={<ProtectedRoute roles={ROLES_AUDITORIA}><Auditoria /></ProtectedRoute>} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}

export default App;
