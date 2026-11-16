import axios from 'axios';

const axiosClient = axios.create({
  // En desarrollo, Vite redirige /api hacia el backend (ver vite.config.js).
  // En despliegue (Vercel) se define VITE_API_URL con la URL pública del backend.
  baseURL: import.meta.env.VITE_API_URL || '/api/v1',
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
});

let renovacionEnCurso = null;
let tokenAcceso = null;

export const guardarTokenAcceso = (token) => {
  tokenAcceso = token;
};

const cerrarSesion = () => {
  guardarTokenAcceso(null);
  localStorage.removeItem('user');
  if (window.location.pathname !== '/login') {
    window.location.assign('/login');
  }
};

const esRutaAuth = (url = '') => /\/auth\/(login|refresh|logout|activar-cuenta)(?:$|[?#])/.test(url);

const renovarToken = () => {
  if (!renovacionEnCurso) {
    renovacionEnCurso = axiosClient
      .post('/auth/refresh', null, {
        headers: { 'X-Auth-Refresh': '1' },
        _skipAuthRefresh: true,
      })
      .then(({ data }) => {
        guardarTokenAcceso(data.token);
        return data.token;
      })
      .finally(() => {
        renovacionEnCurso = null;
      });
  }
  return renovacionEnCurso;
};

/** Devuelve true si el JWT de acceso ya venció. */
export const tokenVencido = (token) => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return typeof payload.exp === 'number' && payload.exp * 1000 <= Date.now();
  } catch {
    return true;
  }
};

axiosClient.interceptors.request.use(async (config) => {
  let token = tokenAcceso;
  if (!config._skipAuthRefresh && !esRutaAuth(config.url) && (!token || tokenVencido(token))) {
    try {
      token = await renovarToken();
    } catch (error) {
      cerrarSesion();
      return Promise.reject(error);
    }
  }
  if (token && !esRutaAuth(config.url)) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axiosClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config;
    if (error.response?.status === 401 && config && !config._retry && !esRutaAuth(config.url)) {
      config._retry = true;
      try {
        const token = await renovarToken();
        config.headers.Authorization = `Bearer ${token}`;
        return axiosClient(config);
      } catch (refreshError) {
        cerrarSesion();
        return Promise.reject(refreshError);
      }
    }
    if (error.response?.status === 401 && config && esRutaAuth(config.url)) {
      return Promise.reject(error);
    }
    if (error.response?.status === 401) {
      cerrarSesion();
    }
    return Promise.reject(error);
  }
);

export default axiosClient;
