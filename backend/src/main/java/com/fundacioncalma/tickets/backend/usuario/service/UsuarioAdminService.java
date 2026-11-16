package com.fundacioncalma.tickets.backend.usuario.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fundacioncalma.tickets.backend.rrhh.cliente.EmpleadoInactivoException;
import com.fundacioncalma.tickets.backend.rrhh.model.Empleado;
import com.fundacioncalma.tickets.backend.rrhh.repository.EmpleadoRepository;
import com.fundacioncalma.tickets.backend.rrhh.service.RrhhClientService;
import com.fundacioncalma.tickets.backend.seguridad.repository.RegistroAccesoRepository;
import com.fundacioncalma.tickets.backend.seguridad.service.AuditoriaUsuarioService;
import com.fundacioncalma.tickets.backend.usuario.dto.UsuarioAdminDTO;
import com.fundacioncalma.tickets.backend.usuario.model.Rol;
import com.fundacioncalma.tickets.backend.usuario.model.Usuario;
import com.fundacioncalma.tickets.backend.usuario.repository.RolRepository;
import com.fundacioncalma.tickets.backend.usuario.repository.RefreshSessionRepository;
import com.fundacioncalma.tickets.backend.usuario.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gestión de cuentas de usuario. Las cuentas se crean a partir de un empleado de RR. HH. (que debe estar activo).
 *
 * Quién puede hacer qué:
 *  - Superadmin (y Director, que equivale a Superadmin): asigna y gestiona cualquier rol.
 *  - Administrador: solo asigna y gestiona Empleado, Técnico TI y Supervisor. No puede crear ni tocar cuentas de
 *    Administrador, Director ni Superadmin (evita que un Administrador se otorgue más poder).
 *  - Nadie puede cambiar el rol ni desactivar su propia cuenta, ni dejar al sistema sin un Superadmin activo.
 *
 * Las cuentas nuevas se activan mediante un enlace de un solo uso enviado al correo corporativo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioAdminService {

    private static final Set<String> ROLES_QUE_ASIGNA_UN_ADMINISTRADOR = Set.of("Empleado", "Técnico TI", "Supervisor");

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EmpleadoRepository empleadoRepository;
    private final RrhhClientService rrhhClientService;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaUsuarioService auditoriaUsuarioService;
    private final RegistroAccesoRepository registroAccesoRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final CuentaActivacionService cuentaActivacionService;

    @Transactional(readOnly = true)
    public List<UsuarioAdminDTO.Cuenta> listar(String busqueda, Authentication actor) {
        Map<String, LocalDateTime> ultimosAccesos = new HashMap<>();
        for (Object[] fila : registroAccesoRepository.findUltimosAccesos()) {
            ultimosAccesos.put((String) fila[0], (LocalDateTime) fila[1]);
        }
        String q = busqueda == null ? "" : busqueda.trim().toLowerCase(Locale.ROOT);

        return usuarioRepository.findAllConDatos().stream()
                .map(u -> aDto(u, actor, ultimosAccesos.get(u.getEmpleado().getCorreoCorporativo().toLowerCase(Locale.ROOT))))
                .filter(c -> q.isEmpty() || c.nombre().toLowerCase(Locale.ROOT).contains(q)
                        || c.correo().toLowerCase(Locale.ROOT).contains(q) || c.rol().toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioAdminDTO.Rol> roles(Authentication actor) {
        return rolRepository.findAll().stream()
                .sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
                .map(r -> new UsuarioAdminDTO.Rol(r.getId(), r.getNombre(), r.getDescripcion(), puedeAsignar(actor, r.getNombre())))
                .toList();
    }

    @Transactional
    public UsuarioAdminDTO.Creado crear(UUID empleadoId, UUID rolId, Authentication actor, String ip) {
        if (empleadoId == null || rolId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el empleado y el rol");
        }
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol indicado no existe"));
        if (!puedeAsignar(actor, rol.getNombre())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permisos para asignar el rol " + rol.getNombre());
        }

        // El empleado debe existir y estar activo en RR. HH.; además queda en la réplica local (clave foránea de la cuenta)
        try {
            if (rrhhClientService.sincronizarEmpleadoPorId(empleadoId).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El empleado indicado no existe en RR. HH.");
            }
        } catch (EmpleadoInactivoException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El empleado indicado no está activo en RR. HH.");
        }
        Empleado empleado = empleadoRepository.findByIdForUpdate(empleadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El empleado indicado no existe"));
        if (usuarioRepository.existeParaEmpleado(empleadoId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El empleado ya tiene una cuenta de usuario");
        }

        Usuario nuevo = crearCuentaPendiente(empleado, rol);
        cuentaActivacionService.enviarEnlace(nuevo.getId());
        auditoriaUsuarioService.registrar(usuarioActor(actor), nuevo, "CREAR_USUARIO", "Rol: " + rol.getNombre(), ip);
        log.info("👤 Cuenta creada para {} con rol {} por {}", empleado.getCorreoCorporativo(), rol.getNombre(), actor.getName());
        return new UsuarioAdminDTO.Creado(aDto(nuevo, actor, null));
    }

    @Transactional
    public boolean crearAutomaticamente(UUID empleadoId) {
        Empleado empleado = empleadoRepository.findByIdForUpdate(empleadoId).orElse(null);
        if (empleado == null
                || empleado.getEstadoLaboral() == null
                || !empleado.getEstadoLaboral().equalsIgnoreCase("ACTIVO")
                || empleado.getCorreoCorporativo() == null
                || empleado.getCorreoCorporativo().isBlank()
                || usuarioRepository.existeParaEmpleado(empleadoId)) {
            return false;
        }

        Rol rolEmpleado = rolRepository.findByNombre("Empleado")
                .orElseThrow(() -> new IllegalStateException("El rol 'Empleado' no está configurado"));
        Usuario nueva = crearCuentaPendiente(empleado, rolEmpleado);
        cuentaActivacionService.enviarEnlace(nueva.getId());
        auditoriaUsuarioService.registrar(null, nueva, "AUTO_CREAR_USUARIO", "Rol: Empleado", null);
        log.info("Cuenta de Empleado aprovisionada para {}", empleado.getCorreoCorporativo());
        return true;
    }

    private Usuario crearCuentaPendiente(Empleado empleado, Rol rol) {
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .rol(rol)
                .empleado(empleado)
                .contrasenaHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .activo(true)
                .activada(false)
                .debeCambiarContrasena(false)
                .build());
    }

    @Transactional
    public void reenviarActivacion(UUID id, Authentication actor, String ip) {
        Usuario usuario = cuentaGestionable(id, actor);
        if (usuario.isActivada()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta ya fue activada");
        }
        cuentaActivacionService.enviarEnlace(id);
        auditoriaUsuarioService.registrar(
                usuarioActor(actor), usuario, "REENVIAR_ACTIVACION", null, ip);
    }

    @Transactional
    public UsuarioAdminDTO.Cuenta actualizar(UUID id, UUID rolId, Boolean activo, Authentication actor, String ip) {
        Usuario usuario = cuentaGestionable(id, actor);
        boolean esActual = esActual(usuario, actor);
        Usuario autor = usuarioActor(actor);
        String rolActual = usuario.getRol().getNombre();

        if (rolId != null && !rolId.equals(usuario.getRol().getId())) {
            if (esActual) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No puede cambiar su propio rol");
            }
            Rol nuevoRol = rolRepository.findById(rolId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol indicado no existe"));
            if (!puedeAsignar(actor, nuevoRol.getNombre())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permisos para asignar el rol " + nuevoRol.getNombre());
            }
            exigirOtroSuperadminActivo(usuario);
            usuario.setRol(nuevoRol);
            refreshSessionRepository.revokeAllForUser(usuario.getId());
            auditoriaUsuarioService.registrar(autor, usuario, "CAMBIO_ROL", "De " + rolActual + " a " + nuevoRol.getNombre(), ip);
        }

        if (activo != null && activo != usuario.isActivo()) {
            if (esActual) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No puede desactivar su propia cuenta");
            }
            if (!activo) {
                exigirOtroSuperadminActivo(usuario);
            }
            usuario.setActivo(activo);
            if (!activo) {
                refreshSessionRepository.revokeAllForUser(usuario.getId());
            }
            auditoriaUsuarioService.registrar(autor, usuario, activo ? "ACTIVAR" : "DESACTIVAR", null, ip);
        }

        return aDto(usuarioRepository.save(usuario), actor, null);
    }

    @Transactional
    public UsuarioAdminDTO.ContrasenaTemporal restablecerContrasena(UUID id, Authentication actor, String ip) {
        Usuario usuario = cuentaGestionable(id, actor);
        if (esActual(usuario, actor)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Para cambiar su propia contraseña use la opción 'Cambiar contraseña'");
        }
        String temporal = GeneradorContrasena.generar();
        usuario.setContrasenaHash(passwordEncoder.encode(temporal));
        usuario.setDebeCambiarContrasena(true);
        usuarioRepository.save(usuario);
        refreshSessionRepository.revokeAllForUser(usuario.getId());
        auditoriaUsuarioService.registrar(usuarioActor(actor), usuario, "RESTABLECER_CONTRASENA", null, ip);
        return new UsuarioAdminDTO.ContrasenaTemporal(temporal);
    }

    // ----- reglas de permisos -----

    /** Un Administrador solo asigna Empleado, Técnico TI y Supervisor; un Superadmin (o Director) asigna cualquier rol. */
    boolean puedeAsignar(Authentication actor, String nombreRol) {
        if (tieneRol(actor, "ROLE_Superadmin")) {
            return true;
        }
        return tieneRol(actor, "ROLE_Administrador") && ROLES_QUE_ASIGNA_UN_ADMINISTRADOR.contains(nombreRol);
    }

    private Usuario cuentaGestionable(UUID id, Authentication actor) {
        Usuario usuario = usuarioRepository.findByIdConDatos(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        // Quien no puede asignar el rol actual de la cuenta tampoco puede modificarla
        if (!puedeAsignar(actor, usuario.getRol().getNombre()) && !esActual(usuario, actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permisos para gestionar una cuenta con el rol " + usuario.getRol().getNombre());
        }
        return usuario;
    }

    /** No se puede quitar al último Superadmin activo (ni desactivándolo ni cambiándole el rol). */
    private void exigirOtroSuperadminActivo(Usuario usuario) {
        if ("Superadmin".equals(usuario.getRol().getNombre()) && usuario.isActivo()
                && usuarioRepository.contarSuperadminsActivos() <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Debe quedar al menos un Superadmin activo");
        }
    }

    private boolean esActual(Usuario usuario, Authentication actor) {
        return usuario.getEmpleado().getCorreoCorporativo().equalsIgnoreCase(actor.getName());
    }

    private boolean tieneRol(Authentication actor, String autoridad) {
        return actor.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(autoridad::equals);
    }

    private Usuario usuarioActor(Authentication actor) {
        return usuarioRepository.findByCorreoCorporativo(actor.getName()).orElse(null);
    }

    private UsuarioAdminDTO.Cuenta aDto(Usuario u, Authentication actor, LocalDateTime ultimoAcceso) {
        Empleado e = u.getEmpleado();
        boolean actual = esActual(u, actor);
        return new UsuarioAdminDTO.Cuenta(
                u.getId(), e.getId(), e.getNombres() + " " + e.getApellidos(), e.getCorreoCorporativo(),
                u.getRol().getId(), u.getRol().getNombre(), u.isActivo(), u.isActivada(),
                u.isDebeCambiarContrasena(), ultimoAcceso,
                !actual && puedeAsignar(actor, u.getRol().getNombre()), actual);
    }
}
