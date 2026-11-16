# Contrato de integración RR. HH. → Sistema de Tickets (v1)

Estado: implementado en ambos sistemas. RR. HH. expone la API de solo lectura y Tickets consume el snapshot y los cambios incrementales
con `RRHH_PROVEEDOR=rest`, `RRHH_API_URL` y `RRHH_API_TOKEN`.

## 1. Alcance
Solo lectura. Incluye consultas individuales, búsqueda y sincronización paginada de empleados. No se crea, modifica ni elimina nada en RR. HH. desde Tickets.

## 2. Autenticación y transporte
- HTTPS, y de preferencia solo desde la red interna.
- Autenticación entre servicios con un **token de servicio** estático en el encabezado `Authorization: Bearer <token>`.
  RR. HH. debe responder `401` si falta o es incorrecto. Rotación recomendada cada 90 días (se cambia en ambos lados).
- Este token es distinto del JWT de los usuarios de RR. HH.: no concede acceso a otros endpoints.

## 3. Objeto `Empleado`
| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `id` | UUID | sí | Identificador estable. **Es el canónico**: tickets lo guarda tal cual en su réplica local. |
| `nombres` | texto | sí | |
| `apellidos` | texto | sí | |
| `correoCorporativo` | texto | sí | Único. Es el que identifica al usuario al iniciar sesión en tickets. |
| `correoPersonal` | texto | no | Correo personal del empleado; puede ser nulo. |
| `tipoDocumento` | texto | no | Tipo de documento de RR. HH.; junto con el número identifica el documento. |
| `numeroDocumento` | texto | no | Corresponde a `empleado.numero_documento` de RR. HH.; Tickets lo guarda en `empleado.dni`. |
| `cargo` | texto | no | Si falta, tickets muestra "No especificado". |
| `estadoLaboral` | texto | sí | `ACTIVO` para quien trabaja hoy; cualquier otro valor se considera no activo (valor configurable en tickets: `rrhh.estado-activo`, sin distinguir mayúsculas). |
| `idDepartamento` | UUID | no | Identificador del departamento de RR. HH. |
| `nombreDepartamento` | texto | no | Nombre del departamento asociado a `idDepartamento`. |
| `fechaIngreso` | fecha ISO-8601 (`YYYY-MM-DD`) | sí | Fecha de ingreso laboral. |
| `liderId` | UUID | no | Jefe directo desde `organigrama.jerarquia.id_jefe`. Nulo si no tiene. |
| `liderCorreo` | texto | no | Correo corporativo del jefe directo. Se usa para la copia de los tickets de prioridad Crítica. |

Tickets ignora los campos adicionales que RR. HH. agregue (la evolución es compatible hacia adelante).
No se solicitan fecha de nacimiento ni teléfonos (minimización de datos personales). El consumidor de Tickets acepta nombres de propiedad tanto `camelCase` como `snake_case` para interoperar con la configuración JSON actual de RR. HH.

## 4. Endpoints (prefijo `/api/integracion/v1`)

### 4.1 Empleado por correo
`GET /empleados/lookup?correo=maria.perez@empresa.org`
- `200` + objeto `Empleado`. `404` si no existe. Con `404` tickets considera que la persona no es un solicitante válido.

### 4.2 Empleado por identificador
`GET /empleados/{id}` (UUID)
- `200` + objeto `Empleado`. `404` si no existe.
- Se usa para mostrar al representante de un área, resolver al jefe directo y validar a quien se designa representante.

### 4.3 Búsqueda
`GET /empleados?q=fernandez&limit=20`
- `200` + arreglo de objetos `Empleado` (puede ser vacío), ordenado por apellidos y nombres.
- `q`: texto que debe buscarse en nombres, apellidos o correo corporativo, sin distinguir mayúsculas ni tildes de forma estricta (basta con `contiene`). Puede venir vacío.
- `limit`: máximo de resultados; acepta valores entre 1 y 500.
- Conviene que devuelva solo empleados activos; de todos modos tickets filtra por `estadoLaboral`.

### 4.4 Snapshot inicial
`GET /empleados/snapshot?limit=500`
- Devuelve `empleados`, `siguienteId`, `cursorCambios` y `hayMas`.
- Para continuar, enviar `afterId=<siguienteId>` y `hastaVersion=<cursorCambios>` hasta que `hayMas=false`.

### 4.5 Cambios incrementales
`GET /empleados/cambios?desde=0&limit=500`
- Devuelve eventos ordenados con `version`, `tipo` (`UPSERT` o `DELETE`), `idEmpleado` y `empleado` (nulo para `DELETE`).
- Tickets guarda el cursor solo después de aplicar los eventos de la página.

### Ejemplo
```
GET /api/integracion/v1/empleados/lookup?correo=maria.perez%40empresa.org
Authorization: Bearer <token>

200 OK
{
  "id": "7d5f8f0e-1111-4222-8333-944455556666",
  "nombres": "María", "apellidos": "Pérez",
  "correoCorporativo": "maria.perez@empresa.org",
  "correoPersonal": "maria.personal@example.org",
  "tipoDocumento": "DNI",
  "numeroDocumento": "12345678",
  "cargo": "Analista de Sistemas",
  "estadoLaboral": "ACTIVO",
  "idDepartamento": "bbbbbbbb-1111-4222-8333-944455556666",
  "nombreDepartamento": "Tecnología",
  "fechaIngreso": "2020-03-15",
  "liderId": "aaaaaaaa-1111-4222-8333-944455556666",
  "liderCorreo": "jefe@empresa.org"
}
```

## 5. Códigos de estado esperados
`200` correcto · `401` token ausente o inválido · `404` empleado inexistente (solo 4.1 y 4.2) · `5xx` error de RR. HH.

## 6. Rendimiento
Las consultas individuales se realizan según su caso de uso y se guardan 5 minutos en memoria. Tickets consume el snapshot inicial y luego
consulta cambios incrementales aproximadamente cada 10 minutos. Si RR. HH. no responde, Tickets sigue funcionando con su copia local (ver sección 7).

## 7. Qué hace el sistema de tickets con estos datos
1. **Crear un ticket**: consulta 4.1 con el correo del usuario. Si no existe → rechaza (403). Si no está activo → rechaza (403).
   Si está activo, guarda o actualiza al empleado en su réplica local (`empleado.empleado`; las claves foráneas de tickets, usuarios y áreas
   la requieren) y continúa. Si RR. HH. no responde, usa la copia local (se registra una advertencia en el log).
2. **Representante de un área** (solo Administrador/Superadmin): busca con 4.3 (solo activos), valida con 4.2 y lo deja en la réplica local.
3. **Copia al jefe en tickets Críticos**: toma `liderCorreo` de 4.2 (con caché). Sin jefe, no se envía copia.
4. **Mostrar al representante** de un área y a quien responde: 4.2, con caché.
5. Identificar al usuario ya autenticado en cada petición usa **solo la copia local** (no consulta a RR. HH.).

## 8. Puesta en marcha
1. Configurar la URL base y el token compartido en ambos backends.
2. En el sistema de tickets: `RRHH_PROVEEDOR=rest`, `RRHH_API_URL=https://.../api/integracion/v1`, `RRHH_API_TOKEN=...`.
   Si falta la URL o el token, la aplicación **no arranca** (falla rápido).
3. **Reconciliar al administrador semilla**: la migración V4 crea a `admin@fundacioncalma.com` con un id aleatorio. Si RR. HH. lo conoce con
   otro id, ejecutar una sola vez `docs/reconciliar_admin.sql` (probado) para que el id de RR. HH. sea el canónico. Si no se hace, el primer
   ticket de esa persona recibe un 409 ("existe localmente con otro identificador").
4. Probar las consultas, el snapshot y los cambios; revisar el estado de sincronización en Tickets.

## 9. Limitaciones y evolución
- Los cambios se reflejan mediante el endpoint incremental de RR. HH.; no se requiere Kafka para esta integración.
- El estado laboral local se comprueba al iniciar sesión y al renovar la sesión. Si RR. HH. reporta una baja, la renovación se rechaza y el
  acceso termina al vencer el JWT actual (máximo 15 minutos). La baja se actualiza al volver a consultar a RR. HH.; la gestión de cuentas de
  usuario de tickets no forma parte de este contrato.
