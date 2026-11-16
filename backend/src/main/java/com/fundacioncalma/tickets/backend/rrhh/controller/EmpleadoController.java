package com.fundacioncalma.tickets.backend.rrhh.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fundacioncalma.tickets.backend.rrhh.dto.EmpleadoDTO;
import com.fundacioncalma.tickets.backend.rrhh.service.RrhhClientService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/empleados")
@RequiredArgsConstructor
public class EmpleadoController {

    private final RrhhClientService rrhhClientService;

    /** Consulta de solo lectura sobre los empleados de RR. HH. Se usa para elegir el representante de un área. */
    @GetMapping
    @PreAuthorize("hasAnyRole('Administrador', 'Superadmin')")
    public ResponseEntity<List<EmpleadoDTO>> buscar(
            @RequestParam(required = false) String q, @RequestParam(required = false) Integer limite) {
        return ResponseEntity.ok(rrhhClientService.buscarEmpleados(q, limite));
    }
}
