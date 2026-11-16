package com.fundacioncalma.tickets.backend.metricas.service;

import com.fundacioncalma.tickets.backend.metricas.dto.DashboardResponse;
import com.fundacioncalma.tickets.backend.metricas.dto.ConteoDTO;
import com.fundacioncalma.tickets.backend.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import com.fundacioncalma.tickets.backend.metricas.dto.ConteoProjection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricasService {

    private final TicketRepository ticketRepository;

    @Cacheable(value = "dashboard", key = "'v2-' + #inicio.toString() + '-' + #fin.toString()")
    public DashboardResponse obtenerMetricasDashboard(LocalDateTime inicio, LocalDateTime fin) {
        log.info("⏳ Calculando métricas desde la Base de Datos para el período: {} a {}", inicio, fin);

        // 1. Obtener conteos agrupados
        var porEstado = ticketRepository.countByEstado(inicio, fin);
        var porPrioridad = ticketRepository.countByPrioridad(inicio, fin);
        var porCategoria = ticketRepository.countByCategoria(inicio, fin);
        var cargaTecnico = ticketRepository.getCargaPorTecnico();

        // 2. Calcular totales simples
        long abiertos = porEstado.stream()
                .filter(e -> !e.getNombre().equals("Cerrado") && !e.getNombre().equals("Cancelado") && !e.getNombre().equals("Resuelto"))
                .mapToLong(ConteoProjection::getCantidad).sum();
                
        long resueltos = porEstado.stream()
                .filter(e -> e.getNombre().equals("Resuelto") || e.getNombre().equals("Cerrado"))
                .mapToLong(ConteoProjection::getCantidad).sum();

        long escalados = porEstado.stream()
                .filter(e -> e.getNombre().equals("Escalado"))
                .mapToLong(ConteoProjection::getCantidad).sum();
        // 3. Calcular % de SLA Cumplido
        double porcentajeSla = 0.0;
        List<Object[]> resultList = ticketRepository.getSlAStats(inicio, fin);
        
        if (resultList != null && !resultList.isEmpty()) {
            // Obtenemos la primera (y única) fila que devuelve el SELECT
            Object[] slaStats = resultList.get(0); 
            
            if (slaStats.length > 0 && slaStats[0] != null) {
                long totalCerrados = ((Number) slaStats[0]).longValue();
                long cumplidos = (slaStats.length > 1 && slaStats[1] != null) ? ((Number) slaStats[1]).longValue() : 0;
                
                if (totalCerrados > 0) {
                    porcentajeSla = (double) cumplidos / totalCerrados * 100;
                }
            }
        }

        // 4. Construir respuesta
        return DashboardResponse.builder()
                .ticketsAbiertos(abiertos)
                .ticketsResueltos(resueltos)
                .ticketsEscalados(escalados)
                .porcentajeSlaCumplido(Math.round(porcentajeSla * 100.0) / 100.0) // Redondeo a 2 decimales
                .ticketsPorEstado(porEstado.stream()
                        .map(e -> new ConteoDTO(e.getNombre(), e.getCantidad()))
                        .toList())
                .ticketsPorPrioridad(porPrioridad.stream()
                        .map(e -> new ConteoDTO(e.getNombre(), e.getCantidad()))
                        .toList())
                .ticketsPorCategoria(porCategoria.stream()
                        .map(e -> new ConteoDTO(e.getNombre(), e.getCantidad()))
                        .toList())
                .cargaPorTecnico(cargaTecnico.stream()
                        .map(e -> new ConteoDTO(e.getNombre(), e.getCantidad()))
                        .toList())
                .build();
    }
}