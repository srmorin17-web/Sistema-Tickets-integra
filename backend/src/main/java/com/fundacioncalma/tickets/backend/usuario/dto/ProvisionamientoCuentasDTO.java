package com.fundacioncalma.tickets.backend.usuario.dto;

public record ProvisionamientoCuentasDTO(
        boolean automatico,
        long elegibles,
        long pendientes,
        long procesando,
        long completados,
        long fallidos) {}
