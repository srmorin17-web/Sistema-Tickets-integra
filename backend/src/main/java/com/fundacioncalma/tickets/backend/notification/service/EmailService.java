package com.fundacioncalma.tickets.backend.notification.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import lombok.extern.slf4j.Slf4j;
// import jakarta.mail.internet.MimeMessage;


@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String remitente;

    public EmailService(JavaMailSender mailSender, @Value("${app.mail.from}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    public void enviarNotificacionCreacionTicket(String destinatario, String codigoTicket, String asuntoTicket, String correoCC) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(destinatario);
            message.setSubject("🎫 Nuevo Ticket Creado: " + codigoTicket);
            message.setText("Hola,\n\n" +
                    "Su solicitud de soporte ha sido registrada exitosamente en el Sistema de Gestión de Tickets de Fundación CALMA.\n\n" +
                    "📌 Código del Ticket: " + codigoTicket + "\n" +
                    "📝 Asunto: " + asuntoTicket + "\n\n" +
                    "Puede dar seguimiento al estado de su solicitud iniciando sesión en el sistema.\n\n" +
                    "Atentamente,\nÁrea de Tecnología - Fundación CALMA");
            
            message.setFrom(remitente);

            if (correoCC != null && !correoCC.isEmpty()) {
            message.setCc(correoCC);
        }

            mailSender.send(message);
            log.info("✅ Correo de notificación enviado a: {} (CC: {})", destinatario, (correoCC == null || correoCC.isEmpty()) ? "ninguno" : correoCC);
        } catch (Exception e) {
            log.error("❌ Error al enviar el correo a {}: {}", destinatario, e.getMessage());
            // Nota: En un sistema real, aquí se podría reencolar el evento o marcar como fallido en BD
        }
    }

    public void enviarEnlaceActivacionCuenta(String destinatario, String nombre, String enlace, long vigenciaHoras) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinatario);
        message.setSubject("Active su cuenta del Sistema de Tickets");
        message.setText("Hola " + nombre + ",\n\n" +
                "Se creó una cuenta para usted en el Sistema de Gestión de Tickets de Fundación CALMA.\n\n" +
                "Para activar su cuenta y elegir su contraseña, abra este enlace:\n" + enlace + "\n\n" +
                "El enlace es de un solo uso y vence en " + vigenciaHoras + " horas. Si no solicitó esta cuenta, ignore este correo.\n\n" +
                "Atentamente,\nFundación CALMA");
        message.setFrom(remitente);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("No se pudo enviar el enlace de activación a {}", destinatario, e);
            throw e;
        }
        log.info("Correo de activación enviado a {}", destinatario);
    }

    /** Aviso al representante del área destino cuando se le escala un ticket. El técnico queda en copia. */
    public void enviarEscalamientoRepresentante(String correoRepresentante, String nombreRepresentante, String codigo,
            String asunto, String prioridad, String area, String motivo, String solicitante, String correoTecnico) {
        enviar(correoRepresentante, correoTecnico,
                "🔼 Ticket " + codigo + " escalado al área " + area,
                "Hola " + nombreRepresentante + ",\n\n" +
                "Se escaló a su área (" + area + ") un ticket que requiere su atención.\n\n" +
                "📌 Código: " + codigo + "\n" +
                "📝 Asunto: " + asunto + "\n" +
                "⚠️ Prioridad: " + prioridad + "\n" +
                "👤 Solicitante: " + solicitante + "\n\n" +
                "Motivo del escalamiento:\n" + motivo + "\n\n" +
                "El técnico responsable del caso está en copia de este correo para coordinar la atención.\n\n" +
                "Atentamente,\nÁrea de Tecnología - Fundación CALMA");
    }

    /** Aviso al solicitante de que su ticket fue derivado a otra área. */
    public void enviarEscalamientoSolicitante(String correoSolicitante, String codigo, String asunto, String area) {
        enviar(correoSolicitante, null,
                "🔄 Su ticket " + codigo + " fue derivado al área " + area,
                "Hola,\n\n" +
                "Su solicitud \"" + asunto + "\" (" + codigo + ") requiere la intervención del área " + area + ".\n" +
                "Seguimos a cargo del caso y le avisaremos cuando haya novedades.\n\n" +
                "Atentamente,\nÁrea de Tecnología - Fundación CALMA");
    }

    /**
     * Aviso al solicitante cuando su ticket cambia a un estado relevante para él.
     * El técnico responsable va en copia para que el solicitante pueda responderle directamente.
     */
    public void enviarCambioEstadoSolicitante(String correoSolicitante, String codigo, String asunto,
            String nuevoEstado, String comentario, String nombreTecnico, String correoTecnico) {
        String tecnico = (nombreTecnico == null || nombreTecnico.isBlank()) ? "el equipo de TI" : nombreTecnico;
        String detalle = (comentario == null || comentario.isBlank()) ? "" : comentario;
        String titulo;
        String cuerpo;

        switch (nuevoEstado) {
            case "En revisión" -> {
                titulo = "🛠️ Su ticket " + codigo + " ya está en atención";
                cuerpo = "Su solicitud ya fue tomada por " + tecnico + " y se encuentra en revisión.";
            }
            case "Requiere información" -> {
                titulo = "❓ Su ticket " + codigo + " requiere información adicional";
                cuerpo = tecnico + " necesita más información para continuar con su solicitud:\n\n" + detalle +
                        "\n\nPuede responder directamente a este correo, donde el técnico está en copia. " +
                        "Mientras tanto, el tiempo de atención se encuentra en pausa.";
            }
            case "Resuelto" -> {
                titulo = "✅ Su ticket " + codigo + " fue resuelto";
                cuerpo = "Su solicitud fue marcada como resuelta.\n\nSolución aplicada:\n" + detalle +
                        "\n\nSi el problema continúa, o si desea dejar un comentario sobre la solución, ingrese al sistema " +
                        "y abra el ticket. Si no hay novedades, se cerrará automáticamente a las 72 horas.";
            }
            case "Cerrado" -> {
                titulo = "🔒 Su ticket " + codigo + " fue cerrado";
                cuerpo = "Su solicitud fue cerrada.\n\n" + detalle;
            }
            default -> {
                return; // los demás estados no se notifican al solicitante
            }
        }

        enviar(correoSolicitante, correoTecnico,
                titulo,
                "Hola,\n\n" + cuerpo + "\n\n" +
                "📌 Código: " + codigo + "\n" +
                "📝 Asunto: " + asunto + "\n\n" +
                "Atentamente,\nÁrea de Tecnología - Fundación CALMA");
    }

    /** Aviso al técnico de que el área destino respondió y el ticket volvió a su bandeja. */
    public void enviarRespuestaArea(String correoTecnico, String codigo, String asunto, String area,
            String representante, String comentario) {
        enviar(correoTecnico, null,
                "↩️ El área " + area + " respondió el ticket " + codigo,
                "Hola,\n\n" +
                "El área " + area + " respondió el ticket escalado y el caso volvió a su bandeja, en revisión.\n\n" +
                "📌 Código: " + codigo + "\n" +
                "📝 Asunto: " + asunto + "\n" +
                "👤 Respondió: " + representante + "\n\n" +
                "Respuesta:\n" + comentario + "\n\n" +
                "Atentamente,\nSistema de Gestión de Tickets - Fundación CALMA");
    }

    /** Aviso al técnico cuando el solicitante reabre un ticket que seguía sin resolverse. */
    public void enviarTicketReabierto(String correoTecnico, String codigo, String asunto,
            String solicitante, String motivo) {
        enviar(correoTecnico, null,
                "🔁 El ticket " + codigo + " fue reabierto por el solicitante",
                "Hola,\n\n" +
                solicitante + " indicó que la solución aplicada no resolvió el problema. El ticket volvió a su bandeja en estado En revisión.\n\n" +
                "📌 Código: " + codigo + "\n" +
                "📝 Asunto: " + asunto + "\n\n" +
                "Motivo de reapertura:\n" + motivo + "\n\n" +
                "Atentamente,\nSistema de Gestión de Tickets - Fundación CALMA");
    }

    private void enviar(String destinatario, String copia, String asunto, String texto) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(destinatario);
            message.setSubject(asunto);
            message.setText(texto);
            message.setFrom(remitente);
            if (copia != null && !copia.isEmpty()) {
                message.setCc(copia);
            }
            mailSender.send(message);
            log.info("✅ Correo enviado a: {} (CC: {}) - {}", destinatario, (copia == null || copia.isEmpty()) ? "ninguno" : copia, asunto);
        } catch (Exception e) {
            log.error("❌ Error al enviar el correo a {}: {}", destinatario, e.getMessage());
        }
    }
}
