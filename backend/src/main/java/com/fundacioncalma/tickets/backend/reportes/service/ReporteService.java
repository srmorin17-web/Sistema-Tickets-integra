package com.fundacioncalma.tickets.backend.reportes.service;

import com.fundacioncalma.tickets.backend.reportes.dto.ReporteTicketDTO;
import com.fundacioncalma.tickets.backend.ticket.repository.TicketRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final TicketRepository ticketRepository;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public List<ReporteTicketDTO> listarTickets(LocalDateTime inicio, LocalDateTime fin, UUID tecnicoId, UUID categoriaId) {
        return ticketRepository.generarDataReporte(inicio, fin, tecnicoId, categoriaId).stream()
                .map(row -> {
                    LocalDateTime creacion = fechaLocal(row[7]);
                    LocalDateTime cierre = fechaLocal(row[8]);
                    LocalDateTime vencimiento = fechaLocal(row[9]);
                    return new ReporteTicketDTO(
                            (String) row[0],
                            (String) row[1],
                            (String) row[2],
                            (String) row[3],
                            (String) row[4],
                            (String) row[5],
                            (String) row[6],
                            formatear(creacion),
                            cierre == null ? "En curso" : formatear(cierre),
                            calcularTiempoResolucion(creacion, cierre),
                            calcularSlaCumplido(cierre, vencimiento));
                })
                .toList();
    }

    public void generarExcel(LocalDateTime inicio, LocalDateTime fin, UUID tecnicoId, UUID categoriaId, OutputStream out) throws Exception {
        List<Object[]> data = ticketRepository.generarDataReporte(inicio, fin, tecnicoId, categoriaId);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Tickets");
            
            // Estilos
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Encabezados
            String[] headers = {"Código", "Asunto", "Solicitante", "Técnico", "Categoría", "Prioridad", "Estado", "F. Creación", "F. Cierre", "T. Resolución", "SLA Cumplido"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data
            int rowNum = 1;
            for (Object[] row : data) {
                Row dataRow = sheet.createRow(rowNum++);
                dataRow.createCell(0).setCellValue((String) row[0]); // Código
                dataRow.createCell(1).setCellValue((String) row[1]); // Asunto
                dataRow.createCell(2).setCellValue((String) row[2]); // Solicitante
                dataRow.createCell(3).setCellValue((String) row[3]); // Técnico
                dataRow.createCell(4).setCellValue((String) row[4]); // Categoría
                dataRow.createCell(5).setCellValue((String) row[5]); // Prioridad
                dataRow.createCell(6).setCellValue((String) row[6]); // Estado
                
// CÓDIGO CORRECTO
                LocalDateTime fCreacion = fechaLocal(row[7]);
                LocalDateTime fCierre = fechaLocal(row[8]);
                LocalDateTime fVencimiento = fechaLocal(row[9]);

                dataRow.createCell(7).setCellValue(fCreacion != null ? fCreacion.format(fmt) : "");
                dataRow.createCell(8).setCellValue(fCierre != null ? fCierre.format(fmt) : "En curso");
                dataRow.createCell(9).setCellValue(calcularTiempoResolucion(fCreacion, fCierre));
                dataRow.createCell(10).setCellValue(calcularSlaCumplido(fCierre, fVencimiento));
            }

            // Autoajustar columnas
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            
            workbook.write(out);
        }
    }

    public void generarPDF(LocalDateTime inicio, LocalDateTime fin, UUID tecnicoId, UUID categoriaId, OutputStream out) throws Exception {
        List<Object[]> data = ticketRepository.generarDataReporte(inicio, fin, tecnicoId, categoriaId);
        
        Document document = new Document(PageSize.A4.rotate()); // Horizontal para que quepan las columnas
        PdfWriter.getInstance(document, out);
        document.open();

        // Encabezado Corporativo
        Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(0, 51, 102));
        Font subtitleFont = new Font(Font.HELVETICA, 10, Font.ITALIC);
        
        document.add(new Paragraph("FUNDACIÓN CALMA", titleFont));
        document.add(new Paragraph("Área de Tecnología - Reporte de Tickets", subtitleFont));
        document.add(new Paragraph("Fecha de emisión: " + LocalDateTime.now().format(fmt), subtitleFont));
        document.add(new Paragraph(" ")); // Espacio

        // Tabla de 11 columnas
        PdfPTable table = new PdfPTable(11);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{8f, 15f, 10f, 10f, 10f, 8f, 8f, 10f, 10f, 10f, 8f});

        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        String[] headers = {"Código", "Asunto", "Solicitante", "Técnico", "Categoría", "Prioridad", "Estado", "F. Creación", "F. Cierre", "T. Resolución", "SLA"};
        
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(new Color(0, 51, 102)); // Azul corporativo
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        Font dataFont = new Font(Font.HELVETICA, 8, Font.NORMAL);
        for (Object[] row : data) {
            LocalDateTime fCreacion = fechaLocal(row[7]);
            LocalDateTime fCierre = fechaLocal(row[8]);
            LocalDateTime fVencimiento = fechaLocal(row[9]);

            addTableCell(table, (String) row[0], dataFont); // Código
            addTableCell(table, (String) row[1], dataFont); // Asunto
            addTableCell(table, (String) row[2], dataFont); // Solicitante
            addTableCell(table, (String) row[3], dataFont); // Técnico
            addTableCell(table, (String) row[4], dataFont); // Categoría
            addTableCell(table, (String) row[5], dataFont); // Prioridad
            addTableCell(table, (String) row[6], dataFont); // Estado
            addTableCell(table, fCreacion != null ? fCreacion.format(fmt) : "", dataFont);
            addTableCell(table, fCierre != null ? fCierre.format(fmt) : "En curso", dataFont);
            addTableCell(table, calcularTiempoResolucion(fCreacion, fCierre), dataFont);
            addTableCell(table, calcularSlaCumplido(fCierre, fVencimiento), dataFont);
        }

        document.add(table);
        document.close();
    }

    // --- Métodos Auxiliares ---
    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(3);
        table.addCell(cell);
    }

    private LocalDateTime fechaLocal(Object valor) {
        if (valor == null) return null;
        if (valor instanceof LocalDateTime fecha) return fecha;
        if (valor instanceof Timestamp fecha) return fecha.toLocalDateTime();
        if (valor instanceof OffsetDateTime fecha) {
            return fecha.atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDateTime();
        }
        if (valor instanceof Instant fecha) return LocalDateTime.ofInstant(fecha, java.time.ZoneId.systemDefault());
        throw new IllegalArgumentException("Tipo de fecha inesperado en el reporte: " + valor.getClass().getName());
    }

    private String formatear(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.toString();
    }

    private String calcularTiempoResolucion(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null) return "En curso";
        long mins = Duration.between(inicio, fin).toMinutes();
        return String.format("%dh %dm", mins / 60, mins % 60);
    }

    private String calcularSlaCumplido(LocalDateTime fCierre, LocalDateTime fVencimiento) {
        if (fCierre == null) return "Pendiente";
        if (fVencimiento == null) return "N/A";
        return fCierre.isBefore(fVencimiento) || fCierre.isEqual(fVencimiento) ? "Sí" : "No";
    }
}