import { Building2, Inbox, LayoutDashboard, FileBarChart, PlusCircle, Settings, ShieldCheck, Ticket, UserCog } from 'lucide-react';

/**
 * El backend maneja seis roles. El frontend los agrupa en cuatro perfiles de
 * navegación, equivalentes a las vistas del prototipo:
 *   Empleado                  -> solicitante
 *   Técnico TI                -> tecnico
 *   Supervisor                -> supervisor
 *   Administrador, Superadmin -> admin
 *                               Director -> admin (tiene los mismos permisos que el Superadmin)
 */
const ROL_A_GRUPO = {
  'Empleado': 'solicitante',
  'Técnico TI': 'tecnico',
  'Supervisor': 'supervisor',
  'Administrador': 'admin',
  'Superadmin': 'admin',
  'Director': 'admin', // el Director tiene los mismos permisos que el Superadmin
};

/** Roles con acceso a la auditoría: el Director tiene los mismos permisos que el Superadmin. */
export const ROLES_AUDITORIA = ['Superadmin', 'Director'];

export const grupoDeRol = (rol) => ROL_A_GRUPO[rol] ?? null;

export const RUTA_INICIO = {
  solicitante: '/mis-tickets',
  tecnico: '/bandeja',
  supervisor: '/dashboard',
  admin: '/dashboard',
};

export const rutaInicio = (grupo) => RUTA_INICIO[grupo] ?? '/login';

/** Opciones de menú lateral por perfil. */
export const NAV_POR_GRUPO = {
  solicitante: [
    { to: '/mis-tickets', label: 'Mis tickets', icon: Ticket },
    { to: '/nuevo-ticket', label: 'Nuevo ticket', icon: PlusCircle },
  ],
  tecnico: [{ to: '/bandeja', label: 'Bandeja', icon: Inbox }],
  supervisor: [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/bandeja', label: 'Bandeja', icon: Inbox },
    { to: '/asignacion', label: 'Asignación', icon: UserCog },
    { to: '/reportes', label: 'Reportes', icon: FileBarChart },
  ],
  admin: [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/todos-tickets', label: 'Todos los tickets', icon: Ticket },
    { to: '/asignacion', label: 'Asignación', icon: UserCog },
    { to: '/reportes', label: 'Reportes', icon: FileBarChart },
    { to: '/administracion', label: 'Administración', icon: Settings },
  ],
};

/**
 * Menú lateral: el perfil base, "Escalados a mi área" si el usuario representa a un área
 * (cualquier rol puede ser representante) y, solo para el Superadmin, la auditoría.
 */
export function menuPara(user, esRepresentante = false) {
  const items = [...(NAV_POR_GRUPO[user.grupo] ?? [])];
  if (esRepresentante) items.push({ to: '/mi-area', label: 'Escalados a mi área', icon: Building2 });
  if (ROLES_AUDITORIA.includes(user.rol)) items.push({ to: '/auditoria', label: 'Auditoría', icon: ShieldCheck });
  return items;
}

export const iniciales = (nombre = '') =>
  nombre
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join('') || 'U';
