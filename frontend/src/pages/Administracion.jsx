import { useEffect, useState } from 'react';
import { CheckCircle2 } from 'lucide-react';
import { obtenerCatalogosAdmin } from '../api/adminApi';
import { mensajeDeError } from '../api/ticketsApi';
import { useCatalogos } from '../context/CatalogosContext';
import { Button, Spinner } from '../components/ui';
import PanelAreas from './admin/PanelAreas';
import PanelCategorias from './admin/PanelCategorias';
import PanelPrioridades from './admin/PanelPrioridades';
import PanelUsuarios from './admin/PanelUsuarios';

const PESTANAS = [
  { id: 'categorias', etiqueta: 'Categorías' },
  { id: 'prioridades', etiqueta: 'Prioridades y SLA' },
  { id: 'areas', etiqueta: 'Áreas de escalamiento' },
  { id: 'usuarios', etiqueta: 'Usuarios' },
];

export default function Administracion() {
  const { recargar: recargarCatalogosGlobales } = useCatalogos();
  const [pestana, setPestana] = useState('categorias');
  const [datos, setDatos] = useState(null);
  const [error, setError] = useState('');
  const [recarga, setRecarga] = useState(0);
  const [aviso, setAviso] = useState('');

  useEffect(() => {
    let activo = true;
    obtenerCatalogosAdmin()
      .then((respuesta) => {
        if (!activo) return;
        setDatos(respuesta);
        setError('');
      })
      .catch((e) => {
        if (activo) setError(mensajeDeError(e, 'No se pudo cargar la información de administración.'));
      });
    return () => {
      activo = false;
    };
  }, [recarga]);

  // Tras guardar se vuelve a consultar y se refrescan los catálogos que usan el resto de pantallas
  const guardado = (mensaje) => {
    setAviso(mensaje);
    setRecarga((n) => n + 1);
    recargarCatalogosGlobales();
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Administración</h1>
        <p className="mt-1 text-sm text-slate-500">Configuración de categorías, prioridades y áreas de escalamiento.</p>
      </div>

      <div className="flex gap-1 overflow-x-auto rounded-lg border border-slate-200 bg-white p-1" role="tablist" aria-label="Secciones de administración">
        {PESTANAS.map((p) => (
          <button
            key={p.id}
            type="button"
            role="tab"
            id={`tab-${p.id}`}
            aria-selected={pestana === p.id}
            aria-controls={`panel-${p.id}`}
            onClick={() => { setPestana(p.id); setAviso(''); }}
            className={`whitespace-nowrap rounded-md px-4 py-2 text-sm font-medium transition-colors ${
              pestana === p.id ? 'bg-sky-600 text-white' : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            {p.etiqueta}
          </button>
        ))}
      </div>

      {aviso && (
        <div role="status" className="flex items-center gap-2 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700 ring-1 ring-emerald-200">
          <CheckCircle2 size={15} /> {aviso}
        </div>
      )}

      {error && (
        <div role="alert" className="flex items-center justify-between gap-3 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200">
          <span>{error}</span>
          <Button size="sm" variant="outline" onClick={() => setRecarga((n) => n + 1)}>Reintentar</Button>
        </div>
      )}

      {!datos && !error && <Spinner label="Cargando configuración..." />}

      {datos && (
        <div role="tabpanel" id={`panel-${pestana}`} aria-labelledby={`tab-${pestana}`}>
          {pestana === 'categorias' && (
            <PanelCategorias categorias={datos.categorias} prioridades={datos.prioridades} onGuardado={guardado} />
          )}
          {pestana === 'prioridades' && <PanelPrioridades prioridades={datos.prioridades} onGuardado={guardado} />}
          {pestana === 'areas' && <PanelAreas areas={datos.areas} onGuardado={guardado} />}
          {pestana === 'usuarios' && <PanelUsuarios />}
        </div>
      )}
    </div>
  );
}
