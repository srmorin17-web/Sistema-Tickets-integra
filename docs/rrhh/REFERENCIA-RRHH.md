# Referencia para implementar el contrato en el proyecto RR. HH. (Spring Boot + Gradle)

**No verificado:** no tuve acceso al código de RR. HH. Los nombres de entidad, repositorio y campos son supuestos y marcados con `TODO`;
adáptelos a su proyecto. No requiere dependencias nuevas.

## 1. DTO (el mismo de las tres consultas)
```java
public record EmpleadoIntegracionDTO(
        java.util.UUID id, String nombres, String apellidos, String correoCorporativo, String cargo,
        String estadoLaboral, java.util.UUID liderId, String liderCorreo) {}
```

## 2. Controlador
```java
@RestController
@RequestMapping("/api/rrhh/v1/empleados")
public class IntegracionEmpleadoController {

    private final EmpleadoRepository empleados; // TODO: su repositorio

    public IntegracionEmpleadoController(EmpleadoRepository empleados) { this.empleados = empleados; }

    @GetMapping("/lookup")
    public ResponseEntity<EmpleadoIntegracionDTO> porCorreo(@RequestParam String correo) {
        return empleados.findByCorreoCorporativoIgnoreCase(correo) // TODO: método equivalente
                .map(e -> ResponseEntity.ok(aDto(e))).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpleadoIntegracionDTO> porId(@PathVariable java.util.UUID id) {
        return empleados.findById(id).map(e -> ResponseEntity.ok(aDto(e))).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<EmpleadoIntegracionDTO> buscar(@RequestParam(defaultValue = "") String q,
                                               @RequestParam(defaultValue = "20") int limit) {
        int tope = Math.max(1, Math.min(limit, 50));
        return empleados.buscarPorTexto(q.trim(), PageRequest.of(0, tope)).stream() // TODO: consulta "contiene" por nombres, apellidos y correo
                .map(this::aDto).toList();
    }

    private EmpleadoIntegracionDTO aDto(Empleado e) { // TODO: ajuste los getters
        Empleado lider = e.getLider();
        return new EmpleadoIntegracionDTO(e.getId(), e.getNombres(), e.getApellidos(), e.getCorreoCorporativo(),
                e.getCargo(), e.getEstadoLaboral(), lider != null ? lider.getId() : null,
                lider != null ? lider.getCorreoCorporativo() : null);
    }
}
```

## 3. Token de servicio
Un filtro que proteja solo `/api/rrhh/v1/**` con un token propio (no el JWT de usuarios):
```java
@Component
public class TokenServicioFilter extends OncePerRequestFilter {

    private final byte[] esperado;

    public TokenServicioFilter(@Value("${rrhh.integracion.token}") String token) {
        this.esperado = ("Bearer " + token).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/rrhh/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String recibido = req.getHeader("Authorization");
        if (recibido == null || !MessageDigest.isEqual(esperado, recibido.getBytes(StandardCharsets.UTF_8))) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        chain.doFilter(req, res);
    }
}
```
En la configuración de seguridad de RR. HH.: permitir `/api/rrhh/v1/**` sin el JWT de usuarios (la autenticación la hace este filtro,
registrado antes del filtro JWT) y definir `rrhh.integracion.token` desde una variable de entorno, nunca en el repositorio.

## 4. Lista de verificación
- [ ] `lookup` devuelve 200 con los 8 campos y 404 para un correo inexistente.
- [ ] `{id}` devuelve el mismo objeto y 404 para un id inexistente.
- [ ] La búsqueda respeta `limit` (máximo 50) y devuelve un arreglo vacío si nada coincide.
- [ ] Sin el encabezado `Authorization` (o con un token incorrecto) los tres responden 401.
- [ ] Los `id` coinciden con los que ya existen en `empleado.empleado` de tickets (ver `docs/reconciliar_admin.sql`).
