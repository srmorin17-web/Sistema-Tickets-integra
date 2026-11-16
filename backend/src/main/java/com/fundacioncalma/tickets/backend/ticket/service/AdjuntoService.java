package com.fundacioncalma.tickets.backend.ticket.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fundacioncalma.tickets.backend.almacenamiento.AlmacenamientoArchivos;
import com.fundacioncalma.tickets.backend.almacenamiento.AlmacenamientoArchivos.ArchivoAlmacenado;
import com.fundacioncalma.tickets.backend.almacenamiento.AlmacenamientoException;
import com.fundacioncalma.tickets.backend.almacenamiento.ArchivoInvalidoException;
import com.fundacioncalma.tickets.backend.almacenamiento.ValidadorArchivo;
import com.fundacioncalma.tickets.backend.rrhh.service.RrhhClientService;
import com.fundacioncalma.tickets.backend.ticket.dto.AdjuntoDTO;
import com.fundacioncalma.tickets.backend.ticket.model.Adjunto;
import com.fundacioncalma.tickets.backend.ticket.model.HistorialAuditoria;
import com.fundacioncalma.tickets.backend.ticket.model.Ticket;
import com.fundacioncalma.tickets.backend.ticket.repository.AdjuntoRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.HistorialAuditoriaRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.TicketRepository;
import com.fundacioncalma.tickets.backend.usuario.model.Usuario;
import com.fundacioncalma.tickets.backend.usuario.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adjuntos de tickets. Políticas (ver application.yml): tamaño máximo, cantidad máxima por ticket,
 * lista blanca de tipos con comprobación del contenido, y retención opcional.
 *
 * Quién puede ver y agregar archivos: el solicitante del ticket, el personal de TI (Técnico TI, Supervisor,
 * Administrador, Superadmin) y el representante del área a la que se escaló. Quién puede eliminar: quien lo subió
 * y los administradores. Con el ticket finalizado (Resuelto o Cerrado) no se agregan ni se eliminan archivos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdjuntoService {

    private static final Set<String> ROLES_PERSONAL = Set.of(
            "ROLE_Técnico TI", "ROLE_Supervisor", "ROLE_Administrador", "ROLE_Superadmin");
    private static final Set<String> ROLES_ADMIN = Set.of("ROLE_Administrador", "ROLE_Superadmin");

    private final AdjuntoRepository adjuntoRepository;
    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialAuditoriaRepository historialRepository;
    private final AlmacenamientoArchivos almacenamiento;
    private final RrhhClientService rrhhClientService;
    private final ObjectMapper objectMapper;

    @Value("${almacenamiento.max-bytes:5242880}")
    private long maxBytes;

    @Value("${almacenamiento.max-adjuntos-por-ticket:5}")
    private int maxPorTicket;

    /** Contenido listo para entregar al navegador. */
    public record Descarga(String nombre, String tipoMime, byte[] contenido) {}

    @Transactional(readOnly = true)
    public List<AdjuntoDTO> listar(UUID ticketId, Authentication auth) {
        Ticket ticket = ticket(ticketId);
        exigirAcceso(ticket, auth);
        boolean finalizado = esFinal(ticket);
        return adjuntoRepository.findByTicketId(ticketId).stream().map(a -> aDto(a, auth, finalizado)).toList();
    }

    @Transactional
    public AdjuntoDTO subir(UUID ticketId, MultipartFile archivo, Authentication auth, String ip) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar un archivo");
        }
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo recibido");
        }

        ValidadorArchivo.Resultado validado;
        try {
            validado = ValidadorArchivo.validar(archivo.getOriginalFilename(), contenido, maxBytes);
        } catch (ArchivoInvalidoException e) {
            throw new ResponseStatusException(estadoPara(e.getMotivo()), e.getMessage());
        }

        Ticket ticket = ticket(ticketId);
        exigirAcceso(ticket, auth);
        if (esFinal(ticket)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El ticket ya finalizó: no se pueden agregar archivos");
        }
        if (adjuntoRepository.contarVigentes(ticketId) >= maxPorTicket) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Se alcanzó el máximo de " + maxPorTicket + " archivos por ticket");
        }

        String clave = ticket.getCodigo() + "/" + UUID.randomUUID() + "_" + validado.nombreSeguro();
        ArchivoAlmacenado guardado;
        try {
            guardado = almacenamiento.guardar(clave, validado.tipoMime(), contenido);
        } catch (AlmacenamientoException e) {
            log.error("❌ No se pudo guardar el adjunto del ticket {}: {}", ticket.getCodigo(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo guardar el archivo");
        }

        try {
            Usuario autor = usuarioRepository.findByCorreoCorporativo(auth.getName()).orElse(null);
            Adjunto adjunto = adjuntoRepository.saveAndFlush(Adjunto.builder()
                    .ticket(ticket)
                    .nombreArchivo(validado.nombreOriginal())
                    .rutaCloudinary(guardado.url())
                    .publicId(guardado.publicId())
                    .tipoMime(validado.tipoMime())
                    .tamanoBytes(contenido.length)
                    .subidoPor(autor)
                    .fechaSubida(LocalDateTime.now())
                    .build());

            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("archivo", validado.nombreOriginal());
            datos.put("bytes", contenido.length);
            registrar(ticket, "ADJUNTO_AGREGADO", datos, autor, ip);

            log.info("📎 Adjunto '{}' agregado al ticket {}", validado.nombreOriginal(), ticket.getCodigo());
            return aDto(adjunto, auth, false);
        } catch (RuntimeException e) {
            // Si falla el registro en la base, no se deja el archivo huérfano en el almacenamiento
            try {
                almacenamiento.eliminar(guardado.publicId());
            } catch (Exception limpieza) {
                log.error("❌ Archivo huérfano en el almacenamiento: {}", guardado.publicId());
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Descarga descargar(UUID adjuntoId, Authentication auth) {
        Adjunto adjunto = adjunto(adjuntoId);
        exigirAcceso(adjunto.getTicket(), auth);
        if (adjunto.isPurgado()) {
            throw new ResponseStatusException(HttpStatus.GONE, "El archivo fue eliminado por la política de retención");
        }
        try {
            return new Descarga(adjunto.getNombreArchivo(), adjunto.getTipoMime(), almacenamiento.descargar(adjunto.getPublicId()));
        } catch (AlmacenamientoException e) {
            log.error("❌ No se pudo descargar el adjunto {}: {}", adjuntoId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo obtener el archivo");
        }
    }

    @Transactional
    public void eliminar(UUID adjuntoId, Authentication auth, String ip) {
        Adjunto adjunto = adjunto(adjuntoId);
        Ticket ticket = adjunto.getTicket();
        exigirAcceso(ticket, auth);
        if (!esAutor(adjunto, auth) && !tieneAlgunRol(auth, ROLES_ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo quien subió el archivo o un administrador puede eliminarlo");
        }
        if (esFinal(ticket)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El ticket ya finalizó: no se pueden eliminar archivos");
        }

        if (!adjunto.isPurgado()) {
            try {
                almacenamiento.eliminar(adjunto.getPublicId());
            } catch (AlmacenamientoException e) {
                log.error("❌ No se pudo eliminar el adjunto {} del almacenamiento: {}", adjuntoId, e.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo eliminar el archivo");
            }
        }
        adjuntoRepository.delete(adjunto);

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("archivo", adjunto.getNombreArchivo());
        registrar(ticket, "ADJUNTO_ELIMINADO", datos, usuarioRepository.findByCorreoCorporativo(auth.getName()).orElse(null), ip);
    }

    // ----- auxiliares -----

    private Ticket ticket(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));
    }

    private Adjunto adjunto(UUID id) {
        return adjuntoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado"));
    }

    private boolean esFinal(Ticket ticket) {
        return Boolean.TRUE.equals(ticket.getEstado().getEsEstadoFinal());
    }

    private void exigirAcceso(Ticket ticket, Authentication auth) {
        boolean esPropio = ticket.getSolicitante().getCorreoCorporativo().equalsIgnoreCase(auth.getName());
        if (!esPropio && !tieneAlgunRol(auth, ROLES_PERSONAL) && !esRepresentanteDelArea(ticket, auth.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a este ticket");
        }
    }

    private boolean esRepresentanteDelArea(Ticket ticket, String correo) {
        if (ticket.getAreaEscalada() == null || ticket.getAreaEscalada().getContactoResponsableId() == null) {
            return false;
        }
        return rrhhClientService.obtenerEmpleadoPorCorreo(correo)
                .map(e -> e.getId().equals(ticket.getAreaEscalada().getContactoResponsableId()))
                .orElse(false);
    }

    private boolean tieneAlgunRol(Authentication auth, Set<String> roles) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(roles::contains);
    }

    private boolean esAutor(Adjunto adjunto, Authentication auth) {
        return adjunto.getSubidoPor() != null && adjunto.getSubidoPor().getEmpleado() != null
                && adjunto.getSubidoPor().getEmpleado().getCorreoCorporativo().equalsIgnoreCase(auth.getName());
    }

    private AdjuntoDTO aDto(Adjunto a, Authentication auth, boolean ticketFinalizado) {
        String autor = a.getSubidoPor() != null && a.getSubidoPor().getEmpleado() != null
                ? a.getSubidoPor().getEmpleado().getNombres() + " " + a.getSubidoPor().getEmpleado().getApellidos()
                : null;
        boolean puedeEliminar = !ticketFinalizado && !a.isPurgado()
                && (esAutor(a, auth) || tieneAlgunRol(auth, ROLES_ADMIN));
        return new AdjuntoDTO(a.getId(), a.getNombreArchivo(), a.getTipoMime(), a.getTamanoBytes(), autor,
                a.getFechaSubida(), puedeEliminar, a.isPurgado());
    }

    private HttpStatus estadoPara(ArchivoInvalidoException.Motivo motivo) {
        return switch (motivo) {
            case MUY_GRANDE -> HttpStatus.PAYLOAD_TOO_LARGE;
            case TIPO_NO_PERMITIDO, CONTENIDO_NO_COINCIDE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private void registrar(Ticket ticket, String tipo, Map<String, Object> datos, Usuario usuario, String ip) {
        try {
            historialRepository.save(HistorialAuditoria.builder()
                    .ticket(ticket)
                    .usuario(usuario)
                    .tipoAccion(tipo)
                    .datosNuevos(objectMapper.writeValueAsString(datos))
                    .timestamp(LocalDateTime.now())
                    .ipOrigen(ip)
                    .build());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la auditoría", e);
        }
    }
}
