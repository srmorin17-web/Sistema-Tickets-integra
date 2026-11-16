package com.fundacioncalma.tickets.backend.ticket.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fundacioncalma.tickets.backend.almacenamiento.AlmacenamientoArchivos;
import com.fundacioncalma.tickets.backend.ticket.model.Adjunto;
import com.fundacioncalma.tickets.backend.ticket.model.HistorialAuditoria;
import com.fundacioncalma.tickets.backend.ticket.repository.AdjuntoRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.HistorialAuditoriaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Política de retención de adjuntos (cuidado del almacenamiento: Cloudinary tiene un cupo de 20 GB).
 * Con almacenamiento.retencion-meses > 0, cada día se eliminan del almacenamiento los archivos de tickets cerrados
 * hace más de ese tiempo. Se conserva el registro (nombre, tamaño, quién y cuándo) marcado como purgado.
 * Con 0 (valor por defecto) no se elimina nada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdjuntosRetencionScheduler {

    private final AdjuntoRepository adjuntoRepository;
    private final AlmacenamientoArchivos almacenamiento;
    private final HistorialAuditoriaRepository historialRepository;
    private final ObjectMapper objectMapper;

    @Value("${almacenamiento.retencion-meses:0}")
    private int retencionMeses;

    @Scheduled(cron = "0 30 3 * * ?") // todos los días a las 3:30
    @Transactional
    public void purgarAdjuntosAntiguos() {
        if (retencionMeses <= 0) {
            return;
        }
        LocalDateTime limite = LocalDateTime.now().minusMonths(retencionMeses);
        int purgados = 0;

        for (Adjunto adjunto : adjuntoRepository.findParaPurgar(limite)) {
            try {
                almacenamiento.eliminar(adjunto.getPublicId());
            } catch (Exception e) {
                log.error("❌ No se pudo purgar el adjunto {}: {}", adjunto.getId(), e.getMessage());
                continue; // se reintenta al día siguiente
            }
            adjunto.setPurgado(true);
            adjunto.setFechaPurgado(LocalDateTime.now());
            adjunto.setRutaCloudinary("");
            adjuntoRepository.save(adjunto);

            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("archivo", adjunto.getNombreArchivo());
            datos.put("motivo", "Política de retención (" + retencionMeses + " meses desde el cierre)");
            try {
                historialRepository.save(HistorialAuditoria.builder()
                        .ticket(adjunto.getTicket())
                        .tipoAccion("ADJUNTO_ELIMINADO")
                        .datosNuevos(objectMapper.writeValueAsString(datos))
                        .timestamp(LocalDateTime.now())
                        .build());
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("No se pudo serializar la auditoría", e);
            }
            purgados++;
        }
        if (purgados > 0) {
            log.info("🧹 Retención de adjuntos: {} archivos eliminados del almacenamiento", purgados);
        }
    }
}
