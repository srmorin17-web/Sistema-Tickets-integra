package com.fundacioncalma.tickets.backend.ticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fundacioncalma.tickets.backend.rrhh.model.Empleado;
import com.fundacioncalma.tickets.backend.rrhh.repository.EmpleadoRepository;
import com.fundacioncalma.tickets.backend.rrhh.service.RrhhClientService;
import com.fundacioncalma.tickets.backend.ticket.event.KafkaProducerService;
import com.fundacioncalma.tickets.backend.ticket.repository.AreaEscalamientoRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.CategoriaRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.EstadoRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.HistorialAuditoriaRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.PrioridadRepository;
import com.fundacioncalma.tickets.backend.ticket.repository.TicketRepository;
import com.fundacioncalma.tickets.backend.usuario.repository.UsuarioRepository;

class TicketServiceTest {

    @Test
    void rechazaCrearTicketCuandoElSolicitanteYaTieneUnoActivo() {
        TicketRepository ticketRepository = mock(TicketRepository.class);
        EmpleadoRepository empleadoRepository = mock(EmpleadoRepository.class);
        RrhhClientService rrhhClientService = mock(RrhhClientService.class);
        UUID empleadoId = UUID.randomUUID();
        Empleado empleado = Empleado.builder().id(empleadoId).build();
        String correo = "empleado@example.org";

        when(rrhhClientService.sincronizarSolicitante(correo)).thenReturn(Optional.of(empleado));
        when(empleadoRepository.findByIdForUpdate(empleadoId)).thenReturn(Optional.of(empleado));
        when(ticketRepository.existeTicketActivoDelSolicitante(empleadoId)).thenReturn(true);

        TicketService service = new TicketService(
                ticketRepository,
                empleadoRepository,
                rrhhClientService,
                mock(SlaCalculatorService.class),
                mock(KafkaProducerService.class),
                mock(CategoriaRepository.class),
                mock(PrioridadRepository.class),
                mock(EstadoRepository.class),
                mock(UsuarioRepository.class),
                mock(AsignacionService.class),
                mock(HistorialAuditoriaRepository.class),
                new ObjectMapper(),
                mock(AreaEscalamientoRepository.class));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.crearTicket(correo, "Asunto", "Descripcion de prueba", UUID.randomUUID()));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(ticketRepository).existeTicketActivoDelSolicitante(empleadoId);
    }
}
