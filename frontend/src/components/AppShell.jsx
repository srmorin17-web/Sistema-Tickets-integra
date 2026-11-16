import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { ChevronDown, KeyRound, LogOut, Menu, Ticket, X } from 'lucide-react';
import { obtenerMisAreas } from '../api/areaApi';
import { useAuth } from '../context/AuthContext';
import { CatalogosProvider } from '../context/CatalogosContext';
import { menuPara } from '../lib/roles';
import { Avatar } from './ui';

function Sidebar({ items, onNavigate }) {
  return (
    <div className="flex h-full flex-col bg-cyan-50">
      <div className="flex items-center gap-2.5 px-5 py-5">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-cyan-600 to-cyan-700 text-white shadow-sm">
          <Ticket size={20} />
        </div>
        <div>
          <p className="text-base font-bold tracking-tight text-slate-900">CALMA</p>
          <p className="text-[10px] font-medium uppercase tracking-wider text-cyan-700">Mesa de Ayuda</p>
        </div>
      </div>
      <nav className="flex-1 space-y-1 px-3 py-2" aria-label="Navegación principal">
        {items.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            onClick={onNavigate}
            className={({ isActive }) =>
              `flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-cyan-700 ${
                isActive ? 'bg-cyan-600 text-white' : 'text-slate-700 hover:bg-cyan-100 hover:text-slate-900'
              }`
            }
          >
            <Icon size={18} />
            {label}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}

export default function AppShell() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef(null);

  const [esRepresentante, setEsRepresentante] = useState(false);
  const items = menuPara(user, esRepresentante);

  // Quien es representante de un área ve "Escalados a mi área" (si la consulta falla, simplemente no aparece)
  useEffect(() => {
    let activo = true;
    obtenerMisAreas()
      .then((areas) => activo && setEsRepresentante(areas.length > 0))
      .catch(() => {});
    return () => {
      activo = false;
    };
  }, []);

  // Cierra el menú de usuario con clic externo o Escape
  useEffect(() => {
    if (!menuOpen) return undefined;
    const onClick = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) setMenuOpen(false);
    };
    const onKey = (e) => e.key === 'Escape' && setMenuOpen(false);
    document.addEventListener('mousedown', onClick);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onClick);
      document.removeEventListener('keydown', onKey);
    };
  }, [menuOpen]);

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="flex h-screen overflow-hidden bg-slate-50">
      <aside className="hidden w-60 shrink-0 border-r border-slate-200 bg-white lg:block">
        <Sidebar items={items} />
      </aside>

      {mobileOpen && (
        <>
          <div className="fixed inset-0 z-40 bg-slate-900/40 lg:hidden" onClick={() => setMobileOpen(false)} />
          <aside className="fixed left-0 top-0 z-50 h-full w-64 border-r border-slate-200 bg-white lg:hidden">
            <button
              type="button"
              aria-label="Cerrar menú"
              onClick={() => setMobileOpen(false)}
              className="absolute right-3 top-4 rounded-lg p-1.5 text-slate-500 hover:bg-cyan-100"
            >
              <X size={18} />
            </button>
            <Sidebar items={items} onNavigate={() => setMobileOpen(false)} />
          </aside>
        </>
      )}

      <div className="flex flex-1 flex-col overflow-hidden">
        <header className="flex h-16 shrink-0 items-center justify-between border-b border-cyan-400 bg-cyan-600 px-4 text-white lg:px-6">
          <div className="flex items-center gap-3">
            <button
              type="button"
              className="rounded-lg p-1 hover:bg-cyan-500 lg:hidden"
              aria-label="Abrir menú"
              onClick={() => setMobileOpen(true)}
            >
              <Menu size={22} />
            </button>
            <div>
              <p className="text-sm font-semibold">{user.rol}</p>
              <p className="text-xs text-cyan-100">Sistema de gestión de tickets</p>
            </div>
          </div>

          <div className="relative" ref={menuRef}>
            <button
              type="button"
              aria-haspopup="menu"
              aria-expanded={menuOpen}
              onClick={() => setMenuOpen((v) => !v)}
              className="flex items-center gap-2 rounded-lg p-1.5 pr-2 hover:bg-cyan-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-white"
            >
              <Avatar initials={user.iniciales} />
              <div className="hidden text-left sm:block">
                <p className="text-sm font-medium">{user.nombre}</p>
                <p className="text-xs text-cyan-100">{user.rol}</p>
              </div>
              <ChevronDown size={16} className="text-cyan-100" />
            </button>
            {menuOpen && (
              <div
                role="menu"
                className="absolute right-0 top-full z-20 mt-1 w-52 overflow-hidden rounded-xl border border-slate-200 bg-white py-1 text-slate-700 shadow-xl"
              >
                <div className="border-b border-slate-100 px-3 py-2 sm:hidden">
                  <p className="truncate text-sm font-medium text-slate-900">{user.nombre}</p>
                  <p className="text-xs text-slate-500">{user.rol}</p>
                </div>
                <button
                  type="button"
                  role="menuitem"
                  onClick={() => navigate('/cambiar-contrasena')}
                  className="flex w-full items-center gap-2 px-3 py-2 text-sm text-slate-700 hover:bg-slate-50"
                >
                  <KeyRound size={15} /> Cambiar contraseña
                </button>
                <button
                  type="button"
                  role="menuitem"
                  onClick={handleLogout}
                  className="flex w-full items-center gap-2 px-3 py-2 text-sm text-rose-600 hover:bg-rose-50"
                >
                  <LogOut size={15} /> Cerrar sesión
                </button>
              </div>
            )}
          </div>
        </header>

        <main className="flex-1 overflow-y-auto">
          <div className="mx-auto max-w-7xl px-4 py-6 lg:px-8">
            <CatalogosProvider>
              <Outlet />
            </CatalogosProvider>
          </div>
        </main>
      </div>
    </div>
  );
}
