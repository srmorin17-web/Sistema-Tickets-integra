package com.fundacioncalma.tickets.backend.reportes.controller;

import com.fundacioncalma.tickets.backend.reportes.dto.ReporteTicketDTO;
import com.fundacioncalma.tickets.backend.reportes.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/tickets")
    @PreAuthorize("hasAnyRole('Supervisor', 'Administrador', 'Superadmin')")
    public List<ReporteTicketDTO> listarTickets(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(required = false) UUID tecnicoId,
            @RequestParam(required = false) UUID categoriaId) {
        return reporteService.listarTickets(inicio, fin, tecnicoId, categoriaId);
    }

    @GetMapping("/tickets/excel")
    @PreAuthorize("hasAnyRole('Supervisor', 'Administrador', 'Superadmin')")
    public void descargarExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(required = false) UUID tecnicoId,
            @RequestParam(required = false) UUID categoriaId,
            HttpServletResponse response) throws Exception {
        
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Reporte_Tickets.xlsx");
        
        reporteService.generarExcel(inicio, fin, tecnicoId, categoriaId, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping("/tickets/pdf")
    @PreAuthorize("hasAnyRole('Supervisor', 'Administrador', 'Superadmin')")
    public void descargarPDF(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(required = false) UUID tecnicoId,
            @RequestParam(required = false) UUID categoriaId,
            HttpServletResponse response) throws Exception {
        
        response.setContentType("application/pdf");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Reporte_Tickets.pdf");
        
        reporteService.generarPDF(inicio, fin, tecnicoId, categoriaId, response.getOutputStream());
        response.flushBuffer();
    }
}