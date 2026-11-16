import axiosClient from './axiosClient';

const datos = (promesa) => promesa.then((r) => r.data);

/** Gestión de cuentas. Administrador y Superadmin (el backend decide qué roles puede asignar cada uno). */
export const listarUsuarios = () => datos(axiosClient.get('/admin/usuarios'));
export const listarRoles = () => datos(axiosClient.get('/admin/roles'));
export const crearUsuario = ({ empleadoId, rolId }) => datos(axiosClient.post('/admin/usuarios', { empleadoId, rolId }));
export const reenviarActivacion = (id) => axiosClient.post(`/admin/usuarios/${id}/reenviar-activacion`);
export const actualizarUsuario = (id, { rolId, activo }) => datos(axiosClient.put(`/admin/usuarios/${id}`, { rolId, activo }));
export const restablecerContrasena = (id) => datos(axiosClient.post(`/admin/usuarios/${id}/restablecer-contrasena`));
export const obtenerProvisionamiento = () => datos(axiosClient.get('/admin/usuarios/provisionamiento'));
export const actualizarProvisionamiento = (automatico) =>
  datos(axiosClient.put('/admin/usuarios/provisionamiento', { automatico }));
export const encolarProvisionamientoMasivo = () =>
  datos(axiosClient.post('/admin/usuarios/provisionamiento/masivo'));

/** El usuario cambia su propia contraseña (única acción permitida mientras tiene una contraseña temporal). */
export const cambiarContrasena = ({ contrasenaActual, contrasenaNueva }) =>
  axiosClient.put('/cuenta/contrasena', { contrasenaActual, contrasenaNueva });

/** Mensaje según el código de estado de las operaciones de cuentas. */
export function mensajeDeUsuarios(error, porDefecto = 'No se pudo completar la operación. Intente nuevamente.') {
  if (!error.response) return 'No hay conexión con el servidor.';
  switch (error.response.status) {
    case 400:
      return 'Los datos no son válidos: el empleado no existe o no está activo en RR. HH., o el rol no existe.';
    case 403:
      return 'No tiene permisos para esta acción sobre esta cuenta o rol.';
    case 404:
      return 'La cuenta ya no existe. Actualice la pantalla.';
    case 409:
      return 'No se puede: el empleado ya tiene cuenta, es su propia cuenta, o es el último Superadmin activo.';
    default:
      return porDefecto;
  }
}
