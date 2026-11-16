import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import axiosClient, { guardarTokenAcceso } from '../api/axiosClient';
import { grupoDeRol, iniciales } from '../lib/roles';

const AuthContext = createContext(null);

const construirUsuario = (nombre, rol, debeCambiar = false) => ({
  nombre,
  rol,
  debeCambiar,
  grupo: grupoDeRol(rol),
  iniciales: iniciales(nombre),
});

const leerSesion = () => {
  const guardado = localStorage.getItem('user');
  if (!guardado) {
    guardarTokenAcceso(null);
    localStorage.removeItem('user');
    return null;
  }
  try {
    const { nombre, rol, debeCambiar } = JSON.parse(guardado);
    return rol ? construirUsuario(nombre, rol, Boolean(debeCambiar)) : null;
  } catch {
    localStorage.removeItem('user');
    return null;
  }
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(leerSesion);

  const login = useCallback(async (correo, contrasena) => {
    let data;
    try {
      ({ data } = await axiosClient.post('/auth/login', { correo, contrasena }));
    } catch (error) {
      if (!error.response) {
        throw new Error('No se pudo conectar con el servidor. Verifique que el backend esté en ejecución.', { cause: error });
      }
      if (error.response.status === 401) {
        throw new Error('Correo o contraseña incorrectos.', { cause: error });
      }
      if (error.response.status === 429) {
        throw new Error('Demasiados intentos fallidos. Espere unos minutos e intente nuevamente.', { cause: error });
      }
      throw new Error('El servidor no pudo procesar el inicio de sesión. Intente nuevamente.', { cause: error });
    }

    if (!grupoDeRol(data.rol)) {
      throw new Error('Su rol no tiene acceso configurado en esta aplicación.');
    }

    guardarTokenAcceso(data.token);
    const debeCambiar = Boolean(data.debeCambiarContrasena);
    localStorage.setItem('user', JSON.stringify({ nombre: data.nombreCompleto, rol: data.rol, debeCambiar }));
    const usuario = construirUsuario(data.nombreCompleto, data.rol, debeCambiar);
    setUser(usuario);
    return usuario;
  }, []);

  const logout = useCallback(async () => {
    try {
      await axiosClient.post('/auth/logout', {}, {
        headers: { 'X-Auth-Refresh': '1' },
        _skipAuthRefresh: true,
      });
    } catch (error) {
      console.error('No se pudo revocar la sesión en el servidor:', error.message);
    } finally {
      guardarTokenAcceso(null);
      localStorage.removeItem('user');
      setUser(null);
    }
  }, []);

  const value = useMemo(() => ({ user, login, logout }), [user, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider');
  return ctx;
};
