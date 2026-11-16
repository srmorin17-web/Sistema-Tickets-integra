package com.fundacioncalma.tickets.backend.rrhh.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fundacioncalma.tickets.backend.rrhh.cliente.RrhhRestCliente;

@Configuration
public class RrhhConfig {

    /** Solo existe con rrhh.proveedor=rest. Falla al arrancar si falta la URL o el token. */
    @Bean
    @ConditionalOnProperty(name = "rrhh.proveedor", havingValue = "rest")
    public RrhhRestCliente rrhhRestCliente(
            @Value("${rrhh.api.url:}") String url,
            @Value("${rrhh.api.token:}") String token,
            @Value("${rrhh.api.timeout-segundos:5}") int timeoutSegundos) {
        if (url.isBlank() || token.isBlank()) {
            throw new IllegalStateException("rrhh.proveedor=rest requiere RRHH_API_URL y RRHH_API_TOKEN");
        }
        return new RrhhRestCliente(url, token, timeoutSegundos);
    }
}
