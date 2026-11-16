package com.fundacioncalma.tickets.backend.ticket.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fundacioncalma.tickets.backend.rrhh.model.Empleado;
import com.fundacioncalma.tickets.backend.ticket.dto.AsignacionDTO;
import com.fundacioncalma.tickets.backend.ticket.model.HistorialAuditoria;
import com.fundacioncalma.tickets.backend.ticket.model.Ticket;
import com.fundacioncalma.tickets.backend.ticket.repository.HistorialAuditoriaRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.TicketRepository;
import com.fundacioncalma.tickets.backend.usuario.model.Usuario;
import com.fundacioncalma.tickets.backend.usuario.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Asignación inteligente (Acta de Refinamiento, sección 4):
 *  - al crear un ticket se elige al técnico con menos tickets activos;
 *  - en caso de empate rota: gana quien lleva más tiempo sin recibir un ticket;
 *  - si se configura un límite por técnico, quienes lo alcanzaron quedan fuera del pool;
 *  - el supervisor puede reasignar manualmente (incluso por encima del límite).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsignacionService {

    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialAuditoriaRepository historialRepository;
    private final ObjectMapper objectMapper;

    /** Máximo de tickets activos por técnico. 0 (valor por defecto) = sin límite. */
    @Value("${tickets.asignacion.max-por-tecnico:0}")
    private int maxPorTecnico;

    private record Carga(Usuario tecnico, long activos, LocalDateTime ultimaAsignacion) {}

    /** Técnico que debe recibir el siguiente ticket; vacío si todos alcanzaron el límite (o no hay técnicos). */
    @Transactional(readOnly = true)
    public Optional<Usuario> elegirTecnico() {
        Comparator<Carga> criterio = Comparator.comparingLong(Carga::activos)
                .thenComparing(Carga::ultimaAsignacion, Comparator.nullsFirst(Comparator.naturalOrder()));

        Optional<Carga> elegido = cargas().stream()
                .filter(c -> maxPorTecnico <= 0 || c.activos() < maxPorTecnico)
                .min(criterio);

        if (elegido.isEmpty()) {
            log.warn("⚠️ Ningún técnico disponible (sin técnicos o todos alcanzaron el límite de {}): el ticket queda sin asignar", maxPorTecnico);
        }
        return elegido.map(Carga::tecnico);
    }

    @Transactional(readOnly = true)
    public List<AsignacionDTO.Tecnico> listarTecnicos() {
        return cargas().stream()
                .map(c -> new AsignacionDTO.Tecnico(
                        c.tecnico().getId(), nombre(c.tecnico().getEmpleado()),
                        c.tecnico().getEmpleado().getCorreoCorporativo(), c.activos(), Math.max(maxPorTecnico, 0)))
                .sorted(Comparator.comparing(AsignacionDTO.Tecnico::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AsignacionDTO.TicketSupervision> listarTicketsActivos() {
        return ticketRepository.findActivosParaSupervision().stream().map(this::aSupervision).toList();
    }

    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true) // cambia la carga por técnico del dashboard
    public AsignacionDTO.TicketSupervision reasignar(
            UUID ticketId, UUID tecnicoId, String motivo, String actorCorreo, String ip) {

        if (tecnicoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el técnico");
        }
        if (motivo != null && motivo.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo no puede superar los 500 caracteres");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));
        if (Boolean.TRUE.equals(ticket.getEstado().getEsEstadoFinal())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El ticket ya finalizó y no puede reasignarse");
        }

        Usuario nuevo = usuarioRepository.findById(tecnicoId)
                .filter(u -> "Técnico TI".equals(u.getRol().getNombre()) && u.isActivo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario indicado no es un técnico activo"));
        Usuario anterior = ticket.getTecnicoAsignado();
        if (anterior != null && anterior.getId().equals(nuevo.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El ticket ya está asignado a ese técnico");
        }

        ticket.setTecnicoAsignado(nuevo);
        ticket.setFechaActualizacion(LocalDateTime.now());
        ticketRepository.save(ticket);

        Map<String, Object> antes = new LinkedHashMap<>();
        antes.put("tecnico", anterior != null ? nombre(anterior.getEmpleado()) : null);
        Map<String, Object> despues = new LinkedHashMap<>();
        despues.put("tecnico", nombre(nuevo.getEmpleado()));
        if (motivo != null && !motivo.isBlank()) {
            despues.put("motivo", motivo.trim());
        }

        // La auditoría forma parte de la misma transacción: si no se puede registrar, no se reasigna
        historialRepository.save(HistorialAuditoria.builder()
                .ticket(ticket)
                .usuario(usuarioRepository.findByCorreoCorporativo(actorCorreo).orElse(null))
                .tipoAccion("REASIGNACION")
                .datosAnteriores(json(antes))
                .datosNuevos(json(despues))
                .timestamp(LocalDateTime.now())
                .ipOrigen(ip)
                .build());

        log.info("🔁 Ticket {} reasignado a {} por {}", ticket.getCodigo(), nombre(nuevo.getEmpleado()), actorCorreo);
        return aSupervision(ticket);
    }

    // ----- auxiliares -----

    private List<Carga> cargas() {
        return ticketRepository.findCargaTecnicos().stream()
                .map(f -> new Carga((Usuario) f[0], ((Number) f[1]).longValue(), (LocalDateTime) f[2]))
                .toList();
    }

    private AsignacionDTO.TicketSupervision aSupervision(Ticket t) {
        Usuario tec = t.getTecnicoAsignado();
        return new AsignacionDTO.TicketSupervision(
                t.getId(), t.getCodigo(), t.getAsunto(),
                nombre(t.getSolicitante()),
                t.getPrioridad().getNombre(), t.getEstado().getNombre(),
                tec != null ? tec.getId() : null,
                tec != null ? nombre(tec.getEmpleado()) : null,
                t.getFechaVencimientoSla(), t.getSlaHorasSnapshot(), Boolean.TRUE.equals(t.getPausado()));
    }

    private String nombre(Empleado e) {
        return e.getNombres() + " " + e.getApellidos();
    }

    private String json(Map<String, Object> datos) {
        try {
            return objectMapper.writeValueAsString(datos);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la auditoría", e);
        }
    }
}
