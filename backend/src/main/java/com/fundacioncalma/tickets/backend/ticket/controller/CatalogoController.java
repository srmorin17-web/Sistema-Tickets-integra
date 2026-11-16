package com.fundacioncalma.tickets.backend.ticket.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fundacioncalma.tickets.backend.ticket.dto.CatalogosResponse;
import com.fundacioncalma.tickets.backend.ticket.service.CatalogoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/catalogos")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    /** Cualquier usuario autenticado puede consultar los catálogos activos. */
    @GetMapping
    public ResponseEntity<CatalogosResponse> obtenerCatalogos() {
        return ResponseEntity.ok(catalogoService.obtenerCatalogos());
    }
}
