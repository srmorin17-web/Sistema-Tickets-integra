package com.fundacioncalma.tickets.backend.shared.config;

import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ticketsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de Tickets API")
                        .description("""
                                API REST del sistema de tickets.
                                Las rutas de integración directa entre Tickets y RR. HH. son de servidor a servidor;
                                su contrato está descrito en docs/contrato-rrhh-v1.md.
                                """)
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSecuritySchemes("refreshCookie", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("refresh_token"))
                        .addSecuritySchemes("refreshHeader", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Auth-Refresh")));
    }

    @Bean
    public OpenApiCustomizer ticketsSecurityDocumentation() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) ->
                pathItem.readOperations().forEach(operation -> {
                    if ("/api/v1/auth/refresh".equals(path) || "/api/v1/auth/logout".equals(path)) {
                        operation.setSecurity(List.of(
                                new SecurityRequirement()
                                        .addList("refreshCookie")
                                        .addList("refreshHeader")));
                    } else if ("/api/v1/auth/login".equals(path)
                            || "/api/v1/auth/activar-cuenta".equals(path)) {
                        operation.setSecurity(List.of());
                    } else {
                        operation.setSecurity(List.of(
                                new SecurityRequirement().addList("bearerAuth")));
                    }
                }));
    }
}
