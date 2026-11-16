// Política de contraseñas. Debe coincidir con PoliticaContrasena del backend (que vuelve a validar todo):
// 10 a 72 caracteres, al menos una letra y un número, distinta de la actual, sin contener el nombre de usuario
// y fuera de una lista de contraseñas conocidas (incluida la de la cuenta semilla).
export const MINIMO = 10;
export const MAXIMO = 72;

const PROHIBIDAS = [
  'admin123!', 'admin123', 'password1', 'password123', 'contraseña1', 'contraseña123',
  '1234567890a', 'qwerty12345', 'calma12345', 'calma2026!',
];

/** Cada requisito con su estado, para mostrarlos como lista de verificación mientras se escribe. */
export function requisitos(nueva, actual = '', correo = '') {
  const usuario = correo.includes('@') ? correo.slice(0, correo.indexOf('@')).toLowerCase() : '';
  return [
    { id: 'largo', texto: `Entre ${MINIMO} y ${MAXIMO} caracteres`, cumple: nueva.length >= MINIMO && new TextEncoder().encode(nueva).length <= MAXIMO },
    { id: 'letra', texto: 'Al menos una letra', cumple: /\p{L}/u.test(nueva) },
    { id: 'numero', texto: 'Al menos un número', cumple: /\d/.test(nueva) },
    { id: 'distinta', texto: 'Distinta de la contraseña actual', cumple: nueva.length > 0 && nueva !== actual },
    {
      id: 'comun',
      texto: 'No es una contraseña común ni contiene su usuario',
      cumple: nueva.length > 0 && !PROHIBIDAS.includes(nueva.toLowerCase()) && !(usuario.length >= 3 && nueva.toLowerCase().includes(usuario)),
    },
  ];
}

export const esValida = (nueva, actual, correo) => requisitos(nueva, actual, correo).every((r) => r.cumple);
