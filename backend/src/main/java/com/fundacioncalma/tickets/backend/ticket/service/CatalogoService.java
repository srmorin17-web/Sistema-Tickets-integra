package com.fundacioncalma.tickets.backend.ticket.service;

import java.util.Comparator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fundacioncalma.tickets.backend.rrhh.dto.EmpleadoDTO;
import com.fundacioncalma.tickets.backend.rrhh.service.RrhhClientService;
import com.fundacioncalma.tickets.backend.ticket.dto.CatalogosResponse;
import com.fundacioncalma.tickets.backend.ticket.model.AreaEscalamiento;
import com.fundacioncalma.tickets.backend.ticket.model.Categoria;
import com.fundacioncalma.tickets.backend.ticket.model.Estado;
import com.fundacioncalma.tickets.backend.ticket.model.Prioridad;
import com.fundacioncalma.tickets.backend.ticket.repository.AreaEscalamientoRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.CategoriaRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.EstadoRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.PrioridadRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final CategoriaRepository categoriaRepository;
    private final PrioridadRepository prioridadRepository;
    private final EstadoRepository estadoRepository;
    private final AreaEscalamientoRepository areaEscalamientoRepository;
    private final RrhhClientService rrhhClientService;

    @Transactional(readOnly = true)
    public CatalogosResponse obtenerCatalogos() {
        var categorias = categoriaRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .sorted(Comparator.comparing(Categoria::getNombre))
                .map(c -> new CatalogosResponse.CategoriaItem(
                        c.getId(), c.getNombre(), c.getDescripcion(),
                        c.getPrioridadDefecto() != null ? c.getPrioridadDefecto().getNombre() : null,
                        c.getPrioridadDefecto() != null ? c.getPrioridadDefecto().getSlaHoras() : null))
                .toList();

        var prioridades = prioridadRepository.findAll().stream()
                .sorted(Comparator.comparing(Prioridad::getSlaHoras))
                .map(p -> new CatalogosResponse.PrioridadItem(p.getId(), p.getNombre(), p.getSlaHoras()))
                .toList();

        var estados = estadoRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getActivo()))
                .sorted(Comparator.comparing(Estado::getOrden))
                .map(e -> new CatalogosResponse.EstadoItem(
                        e.getId(), e.getNombre(), e.getColorHex(), e.getOrden(),
                        Boolean.TRUE.equals(e.getEsEstadoFinal())))
                .toList();

        var areas = areaEscalamientoRepository.findAll().stream()
                .filter(a -> Boolean.TRUE.equals(a.getActivo()))
                .sorted(Comparator.comparing(AreaEscalamiento::getNombre))
                .map(a -> new CatalogosResponse.AreaItem(
                        a.getId(), a.getNombre(), a.getDescripcion(),
                        a.getContactoResponsableId() == null ? null
                                : rrhhClientService.obtenerResumen(a.getContactoResponsableId())
                                        .map(EmpleadoDTO::nombre).orElse(null)))
                .toList();

        return new CatalogosResponse(categorias, prioridades, estados, areas);
    }
}
