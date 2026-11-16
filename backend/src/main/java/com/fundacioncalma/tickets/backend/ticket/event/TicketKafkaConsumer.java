package com.fundacioncalma.tickets.backend.ticket.event;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fundacioncalma.tickets.backend.notification.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketKafkaConsumer {

    private final EmailService emailService;

    @KafkaListener(topics = "ticket.creado", groupId = "tickets-group")
    public void escucharEventoTicketCreado(Map<String, Object> evento) {
        log.info("Evento recibido en tópico 'ticket.creado'. Procesando...");

        try {
            String codigo = (String) evento.get("codigo");
            String correoSolicitante = (String) evento.get("correoSolicitante");
            String asunto = (String) evento.getOrDefault("asunto", "Solicitud de Soporte");
            String correoLider = (String) evento.getOrDefault("correoLider", "");

            if (codigo != null && correoSolicitante != null) {
                log.info("Disparando correo a {} para el ticket {}", correoSolicitante, codigo);
                emailService.enviarNotificacionCreacionTicket(correoSolicitante, codigo, asunto, correoLider);
            } else {
                log.warn("El evento no contiene los datos necesarios (correo o código).");
            }
        } catch (Exception e) {
            log.error("Error al procesar el evento de Kafka: {}", e.getMessage(), e);
        }
    }
    
    @KafkaListener(topics = "ticket.estado_cambiado", groupId = "tickets-group")
    public void escucharEventoEstadoCambiado(Map<String, Object> evento) {
        log.info("Evento recibido en tópico 'ticket.estado_cambiado'. Procesando...");
        try {
            String codigo = (String) evento.get("codigo");
            String correo = (String) evento.get("correoSolicitante");
            String nuevoEstado = (String) evento.get("nuevoEstado");
            String estadoAnterior = (String) evento.getOrDefault("estadoAnterior", "");

            if (codigo == null || correo == null || nuevoEstado == null) {
                log.warn("El evento de cambio de estado no contiene los datos necesarios.");
                return;
            }

            boolean notificar = switch (nuevoEstado) {
                case "Requiere información", "Resuelto", "Cerrado" -> true;
                case "En revisión" -> "Recibido".equals(estadoAnterior);
                default -> false;
            };
            if (!notificar) {
                log.info("El cambio de {} a {} no se notifica al solicitante.", estadoAnterior, nuevoEstado);
                return;
            }

            log.info("Notificando cambio de estado a {} para el ticket {}", correo, codigo);
            emailService.enviarCambioEstadoSolicitante(correo, codigo,
                    (String) evento.getOrDefault("asunto", "Solicitud de Soporte"), nuevoEstado,
                    (String) evento.getOrDefault("comentario", ""),
                    (String) evento.getOrDefault("nombreTecnico", ""),
                    (String) evento.getOrDefault("correoTecnico", ""));
        } catch (Exception e) {
            log.error("Error al procesar evento de cambio de estado: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "ticket.escalado", groupId = "tickets-group")
    public void escucharEventoTicketEscalado(Map<String, Object> evento) {
        try {
            String codigo = (String) evento.get("codigo");
            String asunto = (String) evento.getOrDefault("asunto", "Solicitud de Soporte");
            String areaDestino = (String) evento.get("areaDestino");
            String motivo = (String) evento.getOrDefault("motivo", "");
            String prioridad = (String) evento.getOrDefault("prioridad", "");
            String correoSolicitante = (String) evento.get("correoSolicitante");
            String nombreSolicitante = (String) evento.getOrDefault("nombreSolicitante", correoSolicitante);
            String correoRepresentante = (String) evento.get("correoRepresentante");
            String nombreRepresentante = (String) evento.getOrDefault("nombreRepresentante", "");
            String correoTecnico = (String) evento.getOrDefault("correoTecnico", "");

            log.info("Notificando escalamiento del ticket {} al área {}", codigo, areaDestino);

            // Al representante del área destino (con el técnico en copia)
            if (codigo != null && correoRepresentante != null && !correoRepresentante.isBlank()) {
                emailService.enviarEscalamientoRepresentante(correoRepresentante, nombreRepresentante, codigo, asunto,
                        prioridad, areaDestino, motivo, nombreSolicitante, correoTecnico);
            } else {
                log.warn("El evento de escalamiento no trae el correo del representante del área.");
            }

            // Al solicitante, para que sepa que su ticket fue derivado
            if (codigo != null && correoSolicitante != null && !correoSolicitante.isBlank()) {
                emailService.enviarEscalamientoSolicitante(correoSolicitante, codigo, asunto, areaDestino);
            }
        } catch (Exception e) {
            log.error("Error al procesar el evento de escalamiento: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "ticket.respuesta_area", groupId = "tickets-group")
    public void escucharEventoRespuestaArea(Map<String, Object> evento) {
        try {
            String correoTecnico = (String) evento.getOrDefault("correoTecnico", "");
            if (correoTecnico == null || correoTecnico.isBlank()) {
                log.warn("Respuesta de área sin técnico asignado: no hay a quién notificar.");
                return;
            }
            emailService.enviarRespuestaArea(
                    correoTecnico,
                    (String) evento.get("codigo"),
                    (String) evento.getOrDefault("asunto", "Solicitud de Soporte"),
                    (String) evento.get("area"),
                    (String) evento.get("representante"),
                    (String) evento.getOrDefault("comentario", ""));
        } catch (Exception e) {
            log.error("Error al procesar el evento de respuesta del área: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "ticket.reabierto", groupId = "tickets-group")
    public void escucharEventoTicketReabierto(Map<String, Object> evento) {
        try {
            String correoTecnico = (String) evento.getOrDefault("correoTecnico", "");
            if (correoTecnico == null || correoTecnico.isBlank()) {
                log.warn("Ticket reabierto sin técnico asignado: no hay a quién notificar.");
                return;
            }
            emailService.enviarTicketReabierto(
                    correoTecnico,
                    (String) evento.get("codigo"),
                    (String) evento.getOrDefault("asunto", "Solicitud de soporte"),
                    (String) evento.getOrDefault("solicitante", "El solicitante"),
                    (String) evento.getOrDefault("motivo", ""));
        } catch (Exception e) {
            log.error("Error al procesar evento de reapertura: {}", e.getMessage(), e);
        }
    }
}
