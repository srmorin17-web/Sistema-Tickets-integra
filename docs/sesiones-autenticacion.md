# Sesiones y autenticación

- Los JWT de acceso duran 15 minutos. El frontend los conserva solo en memoria y los envía como `Authorization: Bearer`; tras recargar, obtiene uno nuevo usando la cookie de renovación.
- El refresh token es aleatorio, se rota en cada renovación y solo se conserva en una cookie `HttpOnly`; la base de datos almacena su hash, nunca el valor original.
- La sesión se renueva al necesitar un nuevo token de acceso. Caduca tras 8 horas sin renovación o al cumplir 7 días desde el inicio de sesión.
- Cerrar sesión, cambiar/restablecer contraseña, cambiar el rol o desactivar la cuenta revoca las sesiones de renovación afectadas. Los JWT de acceso ya emitidos pueden funcionar hasta 15 minutos; el estado y rol de una cuenta se verifican en cada petición. Cambiar la contraseña requiere iniciar sesión otra vez.
- Una reutilización de un refresh token ya rotado revoca toda su familia de sesión.
- Las rutas de renovación y cierre de sesión exigen un encabezado personalizado y el CORS del backend solo acepta los orígenes configurados, como protección CSRF para la cookie.

## Variables de entorno

`JWT_SECRET` debe ser una clave aleatoria de al menos 32 bytes codificada en Base64. Para generarla en PowerShell:

```powershell
$bytes = [byte[]]::new(32)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

En desarrollo local, el cookie usa `SameSite=Lax` y no exige HTTPS. Para un frontend y backend en dominios distintos, configura `AUTH_REFRESH_COOKIE_SECURE=true` y `AUTH_REFRESH_COOKIE_SAME_SITE=None`, y sirve ambos por HTTPS. Algunos navegadores pueden bloquear cookies de terceros; para producción es preferible usar un dominio propio compartido o un proxy del mismo sitio.

Los tiempos se pueden ajustar con `JWT_ACCESS_TOKEN_MS`, `JWT_REFRESH_IDLE_MS` y `JWT_REFRESH_ABSOLUTE_MS`. Los valores por defecto son 15 minutos, 8 horas y 7 días, respectivamente.

## Aprovisionamiento de cuentas desde RR. HH.

En el panel de usuarios, un Administrador o Superadmin puede habilitar el aprovisionamiento automático para los empleados que lleguen en futuras sincronizaciones y solicitar una carga masiva de cuentas faltantes para empleados activos ya sincronizados. Las cuentas se crean con el rol `Empleado`, quedan pendientes de activación y reciben un enlace en su correo corporativo. Las cuentas existentes no se modifican.

Las solicitudes masivas se procesan desde una cola persistida en la base de datos. El panel muestra pendientes, en proceso, completadas y fallidas; volver a ejecutar la carga masiva reintenta los elementos fallidos. Los errores de envío no interrumpen la sincronización de RR. HH.

El dashboard de administración permite iniciar una sincronización inmediata con `POST /api/v1/admin/rrhh-sync`. Está disponible para los roles Administrador y Superadmin y tiene un cooldown de dos minutos, aplicado también en el backend. `GET /api/v1/admin/rrhh-sync` continúa consultando el estado y el tiempo restante. La sincronización manual usa el mismo proceso incremental que la programada; si RR. HH. no está disponible, se registra el error y se conservan los datos locales ya sincronizados.

## Documentación de la API

En el perfil local, Swagger UI está disponible en `http://localhost:8081/swagger-ui.html` y la especificación OpenAPI JSON en `http://localhost:8081/v3/api-docs`. La UI documenta los endpoints del backend de Tickets; el contrato de integración de servidor a servidor con RR. HH. permanece en [contrato-rrhh-v1.md](./contrato-rrhh-v1.md).

En Swagger UI, usa **Authorize** para introducir el JWT de acceso en las operaciones protegidas. La renovación y el cierre de sesión usan la cookie `refresh_token` y el encabezado `X-Auth-Refresh: 1`; el navegador administra la cookie HttpOnly, por lo que esas operaciones no requieren copiar su valor manualmente. La especificación representa estos mecanismos por separado.

Fuera del perfil local, la documentación queda deshabilitada por defecto. Se puede controlar explícitamente con `SPRINGDOC_ENABLED`.

El frontend local usa `http://localhost:5174` y la URL de activación predeterminada coincide con ese puerto. Si se usa otro origen para el frontend, configura `APP_FRONTEND_URL` con la URL correcta para que los enlaces de activación apunten al sitio esperado.
