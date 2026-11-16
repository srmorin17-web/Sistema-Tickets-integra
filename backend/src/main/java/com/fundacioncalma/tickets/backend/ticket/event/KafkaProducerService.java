package com.fundacioncalma.tickets.backend.ticket.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void enviarEvento(String topico, Object evento) {
        log.info("📤 Enviando evento al tópico: {} | Datos: {}", topico, evento);
        kafkaTemplate.send(topico, evento);
    }
}
