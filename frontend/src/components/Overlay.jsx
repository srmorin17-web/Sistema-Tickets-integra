import { useEffect, useId, useRef } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';

function useCerrarConEscape(onClose) {
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose();
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [onClose]);
}

/** Panel lateral. Se monta solo cuando está abierto. */
export function Drawer({ onClose, title, subtitle, children, width = 'max-w-2xl' }) {
  const titleId = useId();
  const cerrarRef = useRef(null);
  useCerrarConEscape(onClose);
  useEffect(() => cerrarRef.current?.focus(), []);

  return createPortal(
    <>
      <div className="animate-fade-in fixed inset-0 z-40 bg-slate-900/40" onClick={onClose} />
      <aside
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className={`animate-drawer-in fixed right-0 top-0 z-50 flex h-full w-full ${width} flex-col bg-white shadow-2xl`}
      >
        <header className="flex items-start justify-between border-b border-slate-200 px-6 py-5">
          <div className="min-w-0">
            <h2 id={titleId} className="text-lg font-semibold text-slate-900">{title}</h2>
            {subtitle && <p className="mt-0.5 text-sm text-slate-500">{subtitle}</p>}
          </div>
          <button
            ref={cerrarRef}
            type="button"
            onClick={onClose}
            aria-label="Cerrar"
            className="rounded-lg p-1.5 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600"
          >
            <X size={20} />
          </button>
        </header>
        <div className="flex-1 overflow-y-auto px-6 py-5">{children}</div>
      </aside>
    </>,
    document.body
  );
}

/** Diálogo centrado, por encima del Drawer. Se monta solo cuando está abierto. */
export function Modal({ onClose, title, children, footer, width = 'max-w-lg' }) {
  const titleId = useId();
  useCerrarConEscape(onClose);

  return createPortal(
    <>
      <div className="animate-fade-in fixed inset-0 z-[60] bg-slate-900/40" onClick={onClose} />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className={`animate-fade-in fixed left-1/2 top-1/2 z-[70] w-[calc(100%-2rem)] ${width} -translate-x-1/2 -translate-y-1/2`}
      >
        <div className="overflow-hidden rounded-2xl bg-white shadow-2xl">
          <header className="flex items-center justify-between border-b border-slate-200 px-6 py-4">
            <h2 id={titleId} className="text-base font-semibold text-slate-900">{title}</h2>
            <button
              type="button"
              onClick={onClose}
              aria-label="Cerrar"
              className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
            >
              <X size={18} />
            </button>
          </header>
          <div className="max-h-[60vh] overflow-y-auto px-6 py-5">{children}</div>
          {footer && <footer className="border-t border-slate-200 bg-slate-50 px-6 py-4">{footer}</footer>}
        </div>
      </div>
    </>,
    document.body
  );
}
