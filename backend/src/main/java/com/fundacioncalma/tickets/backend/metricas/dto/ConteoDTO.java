package com.fundacioncalma.tickets.backend.metricas.dto;

import java.io.Serializable;

public record ConteoDTO(String nombre, Long cantidad) implements Serializable {}
