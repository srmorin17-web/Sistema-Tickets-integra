package com.fundacioncalma.tickets.backend.metricas.controller;

import com.fundacioncalma.tickets.backend.metricas.dto.DashboardResponse;
import com.fundacioncalma.tickets.backend.metricas.service.MetricasService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/metricas")
@RequiredArgsConstructor
public class MetricasController {

    private final MetricasService metricasService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        
        DashboardResponse response = metricasService.obtenerMetricasDashboard(inicio, fin);
        return ResponseEntity.ok(response);
    }
}