package com.fundacioncalma.tickets.backend.ticket.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fundacioncalma.tickets.backend.ticket.dto.AdjuntoDTO;
import com.fundacioncalma.tickets.backend.ticket.service.AdjuntoService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/** Los permisos se verifican en AdjuntoService (solicitante, personal de TI o representante del área). */
@RestController
@RequiredArgsConstructor
public class AdjuntoController {

    private final AdjuntoService adjuntoService;

    @GetMapping("/api/v1/tickets/{id}/adjuntos")
    public ResponseEntity<List<AdjuntoDTO>> listar(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(adjuntoService.listar(id, authentication));
    }

    @PostMapping(value = "/api/v1/tickets/{id}/adjuntos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdjuntoDTO> subir(
            @PathVariable UUID id, @RequestParam("archivo") MultipartFile archivo,
            Authentication authentication, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adjuntoService.subir(id, archivo, authentication, http.getRemoteAddr()));
    }

    /** Siempre como descarga (attachment) y con nosniff: el navegador nunca interpreta ni ejecuta el contenido. */
    @GetMapping("/api/v1/adjuntos/{id}/descarga")
    public ResponseEntity<byte[]> descargar(@PathVariable UUID id, Authentication authentication) {
        AdjuntoService.Descarga descarga = adjuntoService.descargar(id, authentication);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(descarga.tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(descarga.nombre(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(descarga.contenido());
    }

    @DeleteMapping("/api/v1/adjuntos/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id, Authentication authentication, HttpServletRequest http) {
        adjuntoService.eliminar(id, authentication, http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
