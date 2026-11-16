package com.fundacioncalma.tickets.backend.metricas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse implements Serializable {
    private long ticketsAbiertos;
    private long ticketsResueltos;
    private long ticketsEscalados;
    private double porcentajeSlaCumplido;
    
    private List<ConteoDTO> ticketsPorEstado;
    private List<ConteoDTO> ticketsPorPrioridad;
    private List<ConteoDTO> ticketsPorCategoria;
    private List<ConteoDTO> cargaPorTecnico;
}