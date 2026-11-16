import { useEffect, useState } from 'react';
import { Search, UserRound, X } from 'lucide-react';
import { buscarEmpleados } from '../api/empleadosApi';

const MIN_CARACTERES = 2;
const ESPERA_MS = 300;

/**
 * Busca un empleado de RR. HH. por nombre o correo y lo elige.
 * `seleccionado`: { id, nombre, correo, cargo } o null. `onChange` recibe el empleado elegido o null.
 */
export default function SelectorEmpleado({ id, seleccionado, onChange }) {
  const [texto, setTexto] = useState('');
  const [resultado, setResultado] = useState({ clave: '', lista: [], error: false });

  const consulta = texto.trim();
  const activa = consulta.length >= MIN_CARACTERES;

  useEffect(() => {
    if (!activa) return undefined;
    let vigente = true;
    const temporizador = setTimeout(() => {
      buscarEmpleados(consulta)
        .then((lista) => vigente && setResultado({ clave: consulta, lista, error: false }))
        .catch(() => vigente && setResultado({ clave: consulta, lista: [], error: true }));
    }, ESPERA_MS);
    return () => {
      vigente = false;
      clearTimeout(temporizador);
    };
  }, [consulta, activa]);

  const buscando = activa && resultado.clave !== consulta;

  if (seleccionado) {
    return (
      <div className="flex items-center justify-between gap-3 rounded-lg border border-slate-300 bg-white px-3 py-2">
        <div className="flex min-w-0 items-center gap-2.5">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-sky-50 text-sky-600">
            <UserRound size={16} />
          </div>
          <div className="min-w-0">
            <p className="truncate text-sm font-medium text-slate-900">{seleccionado.nombre}</p>
            <p className="truncate text-xs text-slate-500">
              {[seleccionado.cargo, seleccionado.correo].filter(Boolean).join(' · ')}
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={() => onChange(null)}
          aria-label="Quitar representante"
          className="rounded-lg p-1.5 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600"
        >
          <X size={16} />
        </button>
      </div>
    );
  }

  return (
    <div>
      <div className="relative">
        <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
        <input
          id={id}
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          placeholder="Buscar por nombre o correo"
          autoComplete="off"
          className="input pl-10"
        />
      </div>
      {activa && (
        <div className="mt-2 max-h-48 overflow-y-auto rounded-lg border border-slate-200 bg-white" aria-live="polite">
          {buscando && <p className="px-3 py-2 text-xs text-slate-500">Buscando...</p>}
          {!buscando && resultado.error && (
            <p className="px-3 py-2 text-xs text-rose-600">No se pudo consultar a los empleados.</p>
          )}
          {!buscando && !resultado.error && resultado.lista.length === 0 && (
            <p className="px-3 py-2 text-xs text-slate-500">Ningún empleado coincide con la búsqueda.</p>
          )}
          {!buscando && resultado.lista.map((e) => (
            <button
              key={e.id}
              type="button"
              onClick={() => {
                onChange(e);
                setTexto('');
              }}
              className="block w-full px-3 py-2 text-left transition-colors hover:bg-slate-50 focus-visible:bg-slate-50 focus-visible:outline-none"
            >
              <span className="block text-sm font-medium text-slate-900">{e.nombre}</span>
              <span className="block text-xs text-slate-500">{[e.cargo, e.correo].filter(Boolean).join(' · ')}</span>
            </button>
          ))}
        </div>
      )}
      {!activa && <p className="mt-1 text-xs text-slate-500">Escriba al menos {MIN_CARACTERES} letras.</p>}
    </div>
  );
}
