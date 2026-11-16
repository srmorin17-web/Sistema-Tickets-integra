package com.fundacioncalma.tickets.backend.usuario.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fundacioncalma.tickets.backend.usuario.repository.UsuarioRepository;
import com.fundacioncalma.tickets.backend.seguridad.service.AuditoriaUsuarioService;

class ProvisionamientoCuentasServiceTest {

    @Test
    void marcaComoCompletadaUnaCuentaAprovisionada() {
        ProvisionamientoColaStore colaStore = mock(ProvisionamientoColaStore.class);
        UsuarioAdminService usuarioAdminService = mock(UsuarioAdminService.class);
        UUID empleadoId = UUID.randomUUID();
        long colaId = 12L;
        when(colaStore.reclamarSiguiente())
                .thenReturn(Optional.of(new ProvisionamientoColaStore.ElementoCola(colaId, empleadoId)));
        when(usuarioAdminService.crearAutomaticamente(empleadoId)).thenReturn(true);

        ProvisionamientoCuentasService service = new ProvisionamientoCuentasService(
                colaStore,
                usuarioAdminService,
                mock(UsuarioRepository.class),
                mock(AuditoriaUsuarioService.class));

        service.procesarSiguiente();

        verify(colaStore).marcarCompletado(colaId, true);
    }

    @Test
    void dejaRegistroFallidoSiNoSePuedeCrearLaCuenta() {
        ProvisionamientoColaStore colaStore = mock(ProvisionamientoColaStore.class);
        UsuarioAdminService usuarioAdminService = mock(UsuarioAdminService.class);
        UUID empleadoId = UUID.randomUUID();
        long colaId = 13L;
        when(colaStore.reclamarSiguiente())
                .thenReturn(Optional.of(new ProvisionamientoColaStore.ElementoCola(colaId, empleadoId)));
        when(usuarioAdminService.crearAutomaticamente(empleadoId))
                .thenThrow(new IllegalStateException("No se pudo enviar el enlace"));

        ProvisionamientoCuentasService service = new ProvisionamientoCuentasService(
                colaStore,
                usuarioAdminService,
                mock(UsuarioRepository.class),
                mock(AuditoriaUsuarioService.class));

        service.procesarSiguiente();

        verify(colaStore).marcarFallido(colaId, "No se pudo enviar el enlace");
    }
}
