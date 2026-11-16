package com.fundacioncalma.tickets.backend.rrhh.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;

import com.fundacioncalma.tickets.backend.rrhh.cliente.RrhhRestCliente;
import com.fundacioncalma.tickets.backend.usuario.service.ProvisionamientoCuentasService;

class RrhhSyncServiceTest {

    @Test
    void rechazaSincronizacionManualDuranteCooldown() {
        ObjectProvider<RrhhRestCliente> clienteProvider = mock(ObjectProvider.class);
        when(clienteProvider.getIfAvailable()).thenReturn(mock(RrhhRestCliente.class));
        RrhhSyncStateStore stateStore = mock(RrhhSyncStateStore.class);
        when(stateStore.reservarSincronizacionManual()).thenReturn(false);
        when(stateStore.cooldownManualSegundos()).thenReturn(90L);
        RrhhSyncService service = crearServicio(clienteProvider, stateStore);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, service::sincronizarAhora);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, error.getStatusCode());
    }

    @Test
    void rechazaSincronizacionManualSiOtraYaEstaEnCurso() {
        ObjectProvider<RrhhRestCliente> clienteProvider = mock(ObjectProvider.class);
        when(clienteProvider.getIfAvailable()).thenReturn(mock(RrhhRestCliente.class));
        RrhhSyncStateStore stateStore = mock(RrhhSyncStateStore.class);
        when(stateStore.reservarSincronizacionManual()).thenReturn(false);
        when(stateStore.cooldownManualSegundos()).thenReturn(0L);
        RrhhSyncService service = crearServicio(clienteProvider, stateStore);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, service::sincronizarAhora);

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void noPermiteSincronizacionManualSiLaIntegracionNoEstaHabilitada() {
        ObjectProvider<RrhhRestCliente> clienteProvider = mock(ObjectProvider.class);
        when(clienteProvider.getIfAvailable()).thenReturn(null);
        RrhhSyncService service = crearServicio(clienteProvider, mock(RrhhSyncStateStore.class));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, service::sincronizarAhora);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
    }

    private RrhhSyncService crearServicio(
            ObjectProvider<RrhhRestCliente> clienteProvider,
            RrhhSyncStateStore stateStore) {
        return new RrhhSyncService(
                clienteProvider,
                stateStore,
                mock(RrhhSincronizador.class),
                mock(ProvisionamientoCuentasService.class),
                mock(PlatformTransactionManager.class),
                500,
                600_000);
    }
}
