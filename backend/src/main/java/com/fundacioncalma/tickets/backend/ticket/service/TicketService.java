package com.fundacioncalma.tickets.backend.ticket.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.fundacioncalma.tickets.backend.rrhh.cliente.EmpleadoInactivoException;
import com.fundacioncalma.tickets.backend.rrhh.dto.EmpleadoDTO;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import com.fundacioncalma.tickets.backend.rrhh.model.Empleado;
import com.fundacioncalma.tickets.backend.rrhh.repository.EmpleadoRepository;
import com.fundacioncalma.tickets.backend.rrhh.service.RrhhClientService;
import com.fundacioncalma.tickets.backend.ticket.event.KafkaProducerService;
import com.fundacioncalma.tickets.backend.ticket.model.AreaEscalamiento;
import com.fundacioncalma.tickets.backend.ticket.model.Categoria;
import com.fundacioncalma.tickets.backend.ticket.model.Estado;
import com.fundacioncalma.tickets.backend.ticket.model.Prioridad;
import com.fundacioncalma.tickets.backend.ticket.model.Ticket;
import com.fundacioncalma.tickets.backend.ticket.repository.AreaEscalamientoRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.CategoriaRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.EstadoRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.PrioridadRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.TicketRepository;
import com.fundacioncalma.tickets.backend.usuario.model.Usuario;
import com.fundacioncalma.tickets.backend.usuario.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fundacioncalma.tickets.backend.ticket.dto.CambiarEstadoRequest;
import com.fundacioncalma.tickets.backend.ticket.dto.EscalarTicketRequest;
import com.fundacioncalma.tickets.backend.ticket.dto.TicketResponseDTO;
import com.fundacioncalma.tickets.backend.ticket.model.HistorialAuditoria;
import com.fundacioncalma.tickets.backend.ticket.repository.HistorialAuditoriaRepository;
import java.time.Duration;
import java.util.HashMap;
import org.springframework.cache.annotation.CacheEvict;
import com.fundacioncalma.tickets.backend.ticket.dto.BandejaTicketDTO;
import org.springframework.security.core.Authentication;


@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final EmpleadoRepository empleadoRepository;
    private final RrhhClientService rrhhClientService;
    private final SlaCalculatorService slaCalculatorService;
    private final KafkaProducerService kafkaProducerService;
    
    // Repositorios auxiliares
    private final CategoriaRepository categoriaRepository;
    private final PrioridadRepository prioridadRepository;
    private final EstadoRepository estadoRepository;

// AGREGAR ESTA LÍNEA:
    private final UsuarioRepository usuarioRepository;
    private final AsignacionService asignacionService;

    private final HistorialAuditoriaRepository historialAuditoriaRepository;
    private final ObjectMapper objectMapper; 

    private final AreaEscalamientoRepository areaEscalamientoRepository;

    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true) 
    public Ticket crearTicket(String correoSolicitante, String asunto, String descripcion, UUID categoriaId) {
        Empleado solicitante;
        try {
            solicitante = rrhhClientService.sincronizarSolicitante(correoSolicitante)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "El solicitante no figura en RR. HH."));
        } catch (EmpleadoInactivoException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El empleado no está activo en RR. HH.");
        }

        solicitante = empleadoRepository.findByIdForUpdate(solicitante.getId())
                .orElseThrow(() -> new IllegalStateException("El solicitante validado ya no existe en la base local."));
        if (ticketRepository.existeTicketActivoDelSolicitante(solicitante.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya tiene un ticket activo. Espere a que sea resuelto o cerrado antes de crear otro.");
        }

        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + categoriaId));
        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría seleccionada no está activa");
        }

        Prioridad prioridad = categoria.getPrioridadDefecto();
        if (prioridad == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "La categoría no tiene una prioridad asignada");
        }
        
        Estado estadoRecibido = estadoRepository.findByNombre("Recibido")
                .orElseThrow(() -> new RuntimeException("El estado 'Recibido' no está configurado en la BD"));

        String fechaHoy = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long consecutivo = ticketRepository.count() + 1; 
        String codigo = "TKT-" + fechaHoy + "-" + String.format("%03d", consecutivo);

        Usuario tecnicoAsignado = asignacionService.elegirTecnico().orElse(null);

        LocalDateTime fechaVencimiento = slaCalculatorService.calcularFechaVencimientoSla(LocalDateTime.now(), prioridad.getSlaHoras());

        Ticket nuevoTicket = Ticket.builder()
                .codigo(codigo)
                .asunto(asunto)
                .descripcion(descripcion)
                .solicitante(solicitante)
                .tecnicoAsignado(tecnicoAsignado)
                .categoria(categoria)
                .prioridad(prioridad)
                .estado(estadoRecibido)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .fechaVencimientoSla(fechaVencimiento)
                .prioridad(prioridad)
                .slaHorasSnapshot(prioridad.getSlaHoras())
                .build();

        Ticket ticketGuardado = ticketRepository.save(nuevoTicket);

// ==========================================
// COPIA AL LÍDER
// ==========================================

String correoLider = null;
if (prioridad.getNombre().equalsIgnoreCase("Crítica")) {
    log.info("Ticket CRÍTICO detectado. Buscando líder jerárquico en RRHH...");
    correoLider = rrhhClientService.obtenerCorreoLider(solicitante.getId()).orElse(null);
    if (correoLider != null) {
        log.info("Líder encontrado: {}. Se enviará en CC.", correoLider);
    }
}

kafkaProducerService.enviarEvento("ticket.creado", Map.of(
        "ticketId", ticketGuardado.getId().toString(),
        "codigo", ticketGuardado.getCodigo(),
        "asunto", ticketGuardado.getAsunto(),
        "correoSolicitante", solicitante.getCorreoCorporativo(),
        "correoLider", correoLider != null ? correoLider : "", // <-- NUEVO CAMPO
        "tecnicoAsignado", tecnicoAsignado != null ? tecnicoAsignado.getEmpleado().getCorreoCorporativo() : "Bandeja General"
)); 

        log.info("Ticket {} creado exitosamente. Asignado a: {}", codigo, 
                 tecnicoAsignado != null ? tecnicoAsignado.getEmpleado().getNombres() : "Sin asignar");
        
        return ticketGuardado;
    }

    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true) // <-- AGREGAR ESTO
    public Ticket actualizarEstado(UUID ticketId, CambiarEstadoRequest request, String usuarioNombre, String ipOrigen) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado con ID: " + ticketId));

        Estado estadoAnterior = ticket.getEstado();
        Estado nuevoEstado = estadoRepository.findById(request.getNuevoEstadoId())
                .orElseThrow(() -> new RuntimeException("Estado no encontrado"));

        if ((nuevoEstado.getNombre().equals("Resuelto")
                || nuevoEstado.getNombre().equals("Cancelado")
                || nuevoEstado.getNombre().equals("Requiere información"))
            && (request.getComentario() == null || request.getComentario().trim().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Es obligatorio agregar un comentario al cambiar el ticket a " + nuevoEstado.getNombre() + ".");
        }

        if (nuevoEstado.getNombre().equals("Requiere información")) {
            ticket.setPausado(true);
            ticket.setFechaPausaSla(LocalDateTime.now());
            log.info("SLA pausado para el ticket {}", ticket.getCodigo());
        } else if ((estadoAnterior.getNombre().equals("Requiere información") || estadoAnterior.getNombre().equals("Escalado"))
                && !nuevoEstado.getNombre().equals("Requiere información")) {
            // Reanudación (al salir de "Requiere información" o de "Escalado"): Sumamos el tiempo pausado a la fecha de vencimiento para no penalizar al técnico
            if (ticket.getFechaPausaSla() != null) {
                // Solo se descuentan las horas laborales de la pausa (L-V 8:00 a 17:00), no el tiempo de calendario
                long minutosPausados = slaCalculatorService.minutosLaboralesEntre(ticket.getFechaPausaSla(), LocalDateTime.now());
                ticket.setFechaVencimientoSla(slaCalculatorService.sumarMinutosLaborales(ticket.getFechaVencimientoSla(), minutosPausados));
                log.info("SLA reanudado para el ticket {}. Se extendió el vencimiento en {} minutos laborales.", ticket.getCodigo(), minutosPausados);
            }
            ticket.setPausado(false);
            ticket.setFechaPausaSla(null);
        }

        // 3. Actualizar el ticket
        ticket.setEstado(nuevoEstado);
        ticket.setFechaActualizacion(LocalDateTime.now());

        // El historial es también la fuente del comentario visible para el solicitante y el técnico.
        // No se debe confirmar el cambio de estado si este registro no pudo guardarse.
        Map<String, Object> datosAnteriores = Map.of(
                "estado", estadoAnterior.getNombre(),
                "fechaActualizacion", ticket.getFechaActualizacion().toString()
        );
        Map<String, Object> datosNuevos = Map.of(
                "estado", nuevoEstado.getNombre(),
                "comentario", request.getComentario() == null ? "" : request.getComentario().trim()
        );
        final String jsonAnterior;
        final String jsonNuevo;
        try {
            jsonAnterior = objectMapper.writeValueAsString(datosAnteriores);
            jsonNuevo = objectMapper.writeValueAsString(datosNuevos);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudieron serializar los datos del historial del ticket", e);
        }

        HistorialAuditoria auditoria = HistorialAuditoria.builder()
                .ticket(ticket)
                .usuario(usuarioRepository.findByCorreoCorporativo(usuarioNombre).orElse(null))
                .tipoAccion("CAMBIO_ESTADO")
                .estadoAnteriorId(estadoAnterior.getId())
                .estadoNuevoId(nuevoEstado.getId())
                .datosAnteriores(jsonAnterior)
                .datosNuevos(jsonNuevo)
                .timestamp(LocalDateTime.now())
                .ipOrigen(ipOrigen)
                .build();
        historialAuditoriaRepository.save(auditoria);

        Ticket ticketActualizado = ticketRepository.save(ticket);

        // 5. Publicar evento a Kafka para notificar al solicitante
        kafkaProducerService.enviarEvento("ticket.estado_cambiado", Map.of(
                "ticketId", ticketActualizado.getId().toString(),
                "codigo", ticketActualizado.getCodigo(),
                "asunto", ticketActualizado.getAsunto(),
                "nuevoEstado", nuevoEstado.getNombre(),
                "comentario", request.getComentario() != null ? request.getComentario() : "",
                "correoSolicitante", ticketActualizado.getSolicitante().getCorreoCorporativo(),
                "estadoAnterior", estadoAnterior.getNombre(),
                "nombreTecnico", ticketActualizado.getTecnicoAsignado() != null
                        ? ticketActualizado.getTecnicoAsignado().getEmpleado().getNombres() + " "
                                + ticketActualizado.getTecnicoAsignado().getEmpleado().getApellidos() : "",
                "correoTecnico", ticketActualizado.getTecnicoAsignado() != null
                        ? ticketActualizado.getTecnicoAsignado().getEmpleado().getCorreoCorporativo() : ""
        ));

        log.info("Ticket {} actualizado a estado: {} por {}", ticket.getCodigo(), nuevoEstado.getNombre(), usuarioNombre);
        return ticketActualizado;
    }

    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public Ticket responderSolicitudInformacion(UUID ticketId, String comentario, String correoSolicitante, String ipOrigen) {
        if (comentario == null || comentario.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La información adicional es obligatoria");
        }
        String respuesta = comentario.trim();
        if (respuesta.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La respuesta no puede superar los 1000 caracteres");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));
        if (!ticket.getSolicitante().getCorreoCorporativo().equalsIgnoreCase(correoSolicitante)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el solicitante puede responder esta solicitud");
        }
        if (!"Requiere información".equals(ticket.getEstado().getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El ticket ya no está esperando información");
        }

        Estado enRevision = estadoRepository.findByNombre("En revisión")
                .orElseThrow(() -> new IllegalStateException("Estado 'En revisión' no configurado"));
        CambiarEstadoRequest request = new CambiarEstadoRequest();
        request.setNuevoEstadoId(enRevision.getId());
        request.setComentario(respuesta);
        return actualizarEstado(ticketId, request, correoSolicitante, ipOrigen);
    }

// ==========================================
// MÉTODO 1: ESCALAR TICKET (RF-21, RF-22)
// ==========================================
@Transactional
@CacheEvict(value = "dashboard", allEntries = true)
public TicketResponseDTO escalarTicket(UUID ticketId, EscalarTicketRequest request, String usuarioNombre, String ipOrigen) { // <-- CAMBIO 1: Retorna DTO
    if (request.getAreaEscalamientoId() == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar el área de destino");
    }
    if (request.getMotivo() == null || request.getMotivo().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo del escalamiento es obligatorio");
    }

    Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));
    if (Boolean.TRUE.equals(ticket.getEstado().getEsEstadoFinal())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "El ticket ya finalizó y no puede escalarse");
    }

    AreaEscalamiento area = areaEscalamientoRepository.findById(request.getAreaEscalamientoId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Área de escalamiento no encontrada"));
    if (!Boolean.TRUE.equals(area.getActivo())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "El área seleccionada no está activa");
    }

    // Cada área tiene un representante (empleado de RR. HH.) que recibe los tickets escalados
    EmpleadoDTO representante = area.getContactoResponsableId() == null ? null
            : rrhhClientService.obtenerResumen(area.getContactoResponsableId()).orElse(null);
    if (representante == null) {
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "El área seleccionada no tiene un representante asignado");
    }

    Estado estadoAnterior = ticket.getEstado();
    Estado estadoEscalado = estadoRepository.findByNombre("Escalado")
            .orElseThrow(() -> new RuntimeException("Estado 'Escalado' no configurado"));

    
    ticket.setPausado(true);
    ticket.setFechaPausaSla(LocalDateTime.now());
    
    
    ticket.setAreaEscalada(area);
    ticket.setMotivoEscalamiento(request.getMotivo());
    ticket.setEstado(estadoEscalado);
    ticket.setFechaActualizacion(LocalDateTime.now());

    
    try {
        HistorialAuditoria auditoria = HistorialAuditoria.builder()
                .ticket(ticket)
                .usuario(usuarioRepository.findByCorreoCorporativo(usuarioNombre).orElse(null))
                .tipoAccion("ESCALAMIENTO")
                .estadoAnteriorId(estadoAnterior.getId())
                .estadoNuevoId(estadoEscalado.getId())
                .datosNuevos(objectMapper.writeValueAsString(Map.of("area", area.getNombre(), "motivo", request.getMotivo(), "representante", representante.nombre())))
                .timestamp(LocalDateTime.now())
                .ipOrigen(ipOrigen)
                .build();
        historialAuditoriaRepository.save(auditoria);
    } catch (Exception e) { log.error("Error en auditoría de escalamiento", e); }

    Ticket ticketActualizado = ticketRepository.save(ticket);

    Map<String, Object> eventoEscalado = new HashMap<>();
    eventoEscalado.put("codigo", ticketActualizado.getCodigo());
    eventoEscalado.put("asunto", ticketActualizado.getAsunto());
    eventoEscalado.put("prioridad", ticketActualizado.getPrioridad().getNombre());
    eventoEscalado.put("areaDestino", area.getNombre());
    eventoEscalado.put("motivo", request.getMotivo());
    eventoEscalado.put("correoSolicitante", ticketActualizado.getSolicitante().getCorreoCorporativo());
    eventoEscalado.put("nombreSolicitante", ticketActualizado.getSolicitante().getNombres() + " " + ticketActualizado.getSolicitante().getApellidos());
    eventoEscalado.put("nombreRepresentante", representante.nombre());
    eventoEscalado.put("correoRepresentante", representante.correo());
    eventoEscalado.put("correoTecnico", ticketActualizado.getTecnicoAsignado() != null
            ? ticketActualizado.getTecnicoAsignado().getEmpleado().getCorreoCorporativo() : "");
    kafkaProducerService.enviarEvento("ticket.escalado", eventoEscalado);

    return TicketResponseDTO.builder()
            .id(ticketActualizado.getId())
            .codigo(ticketActualizado.getCodigo())
            .asunto(ticketActualizado.getAsunto())
            .estado(ticketActualizado.getEstado().getNombre()) 
            .categoria(ticketActualizado.getCategoria().getNombre())
            .prioridad(ticketActualizado.getPrioridad().getNombre())
            .areaEscalada(ticketActualizado.getAreaEscalada().getNombre())
            .fechaCreacion(ticketActualizado.getFechaCreacion())
            .build();
}

@Transactional(readOnly = true)
public List<BandejaTicketDTO> obtenerBandejaTecnico(Authentication authentication) {
    
    String correo = authentication.getName();
    Usuario tecnico = usuarioRepository.findByCorreoCorporativo(correo)
            .orElseThrow(() -> new RuntimeException("Técnico no encontrado"));

    List<Ticket> tickets = ticketRepository.findBandejaPorTecnico(tecnico.getId());

    return tickets.stream().map(t -> BandejaTicketDTO.builder()
            .id(t.getId())
            .codigo(t.getCodigo())
            .asunto(t.getAsunto())
            .solicitante(t.getSolicitante().getNombres() + " " + t.getSolicitante().getApellidos())
            .prioridad(t.getPrioridad().getNombre())
            .estado(t.getEstado().getNombre())
            .fechaVencimientoSla(t.getFechaVencimientoSla())
            .slaHorasSnapshot(t.getSlaHorasSnapshot())
            .pausado(t.getPausado())
            .build()).toList();
}

    
}