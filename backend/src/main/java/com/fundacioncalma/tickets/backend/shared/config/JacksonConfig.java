package com.fundacioncalma.tickets.backend.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class JacksonConfig {

    /**
     * Crea y expone el bean de ObjectMapper.
     * Usamos findAndRegisterModules() para que detecte automáticamente 
     * el JavaTimeModule (fechas) si está en las dependencias.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules(); 
        return mapper;
    }
}