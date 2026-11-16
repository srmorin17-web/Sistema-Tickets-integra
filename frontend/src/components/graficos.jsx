import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { BarChart3 } from 'lucide-react';
import { PALETA } from '../lib/colores';
import { EmptyState } from './ui';

const TOOLTIP = { fontSize: 12, borderRadius: 8 };
const EJE = { fontSize: 11, fill: '#64748b' };

const recortar = (texto, max = 18) => (texto.length > max ? `${texto.slice(0, max - 1)}…` : texto);

/** Tabla solo para lectores de pantalla: los gráficos de Recharts no exponen sus valores. */
function TablaAccesible({ titulo, datos }) {
  return (
    <table className="sr-only">
      <caption>{titulo}</caption>
      <thead>
        <tr><th>Elemento</th><th>Cantidad</th></tr>
      </thead>
      <tbody>
        {datos.map((d) => (
          <tr key={d.nombre}><td>{d.nombre}</td><td>{d.cantidad}</td></tr>
        ))}
      </tbody>
    </table>
  );
}

function SinDatos() {
  return <EmptyState icon={<BarChart3 size={20} />} title="Sin datos" desc="No hay registros para mostrar." />;
}

/** Barras verticales u horizontales a partir de [{ nombre, cantidad }]. */
export function GraficoBarras({ titulo, datos, colorDe, horizontal = false, alto = 240 }) {
  if (!datos?.length) return <SinDatos />;
  const color = (nombre, i) => colorDe?.[nombre] ?? PALETA[i % PALETA.length];

  return (
    <div>
      <div role="img" aria-label={titulo}>
        <ResponsiveContainer width="100%" height={horizontal ? Math.max(alto, datos.length * 40) : alto}>
          <BarChart
            data={datos}
            layout={horizontal ? 'vertical' : 'horizontal'}
            margin={horizontal ? { left: 8, right: 16 } : { left: -16, right: 8 }}
          >
            <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" horizontal={!horizontal} vertical={horizontal} />
            {horizontal ? (
              <>
                <XAxis type="number" allowDecimals={false} tick={EJE} />
                <YAxis type="category" dataKey="nombre" width={130} tick={EJE} tickFormatter={(v) => recortar(v)} />
              </>
            ) : (
              <>
                <XAxis dataKey="nombre" tick={EJE} tickFormatter={(v) => recortar(v, 14)} interval={0} />
                <YAxis allowDecimals={false} tick={EJE} />
              </>
            )}
            <Tooltip contentStyle={TOOLTIP} cursor={{ fill: '#f1f5f9' }} />
            <Bar dataKey="cantidad" name="Tickets" radius={horizontal ? [0, 4, 4, 0] : [4, 4, 0, 0]}>
              {datos.map((d, i) => (
                <Cell key={d.nombre} fill={color(d.nombre, i)} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
      <TablaAccesible titulo={titulo} datos={datos} />
    </div>
  );
}

/** Gráfico de dona con leyenda numérica. */
export function GraficoDona({ titulo, datos, alto = 200 }) {
  if (!datos?.length) return <SinDatos />;
  const total = datos.reduce((suma, d) => suma + d.cantidad, 0);

  return (
    <div>
      <div role="img" aria-label={titulo}>
        <ResponsiveContainer width="100%" height={alto}>
          <PieChart>
            <Pie data={datos} dataKey="cantidad" nameKey="nombre" innerRadius={50} outerRadius={80} paddingAngle={2}>
              {datos.map((d, i) => (
                <Cell key={d.nombre} fill={PALETA[i % PALETA.length]} />
              ))}
            </Pie>
            <Tooltip contentStyle={TOOLTIP} />
          </PieChart>
        </ResponsiveContainer>
      </div>
      <ul className="mt-2 space-y-1">
        {datos.map((d, i) => (
          <li key={d.nombre} className="flex items-center justify-between gap-2 text-xs text-slate-600">
            <span className="flex min-w-0 items-center gap-1.5">
              <span className="h-2 w-2 shrink-0 rounded-full" style={{ backgroundColor: PALETA[i % PALETA.length] }} />
              <span className="truncate">{d.nombre}</span>
            </span>
            <span className="shrink-0 font-medium text-slate-700">
              {d.cantidad} ({total ? Math.round((d.cantidad / total) * 100) : 0}%)
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
