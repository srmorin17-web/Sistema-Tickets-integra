import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { obtenerCatalogos } from '../api/ticketsApi';

const CatalogosContext = createContext(null);

/**
 * Carga una vez los catálogos (estados, categorías, prioridades, áreas) desde GET /catalogos.
 * Se monta dentro de AppShell, por lo que se reinicia al cerrar sesión.
 */
export function CatalogosProvider({ children }) {
  const [catalogos, setCatalogos] = useState(null);
  const [error, setError] = useState('');
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    let activo = true;
    obtenerCatalogos()
      .then((datos) => {
        if (!activo) return;
        setCatalogos(datos);
        setError('');
      })
      .catch(() => {
        if (activo) setError('No se pudieron cargar los catálogos del sistema.');
      });
    return () => {
      activo = false;
    };
  }, [intento]);

  const recargar = useCallback(() => setIntento((n) => n + 1), []);
  const value = useMemo(() => ({ catalogos, error, recargar }), [catalogos, error, recargar]);

  return <CatalogosContext.Provider value={value}>{children}</CatalogosContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useCatalogos() {
  const ctx = useContext(CatalogosContext);
  if (!ctx) throw new Error('useCatalogos debe usarse dentro de CatalogosProvider');
  return ctx;
}
