package com.fundacioncalma.tickets.backend.usuario.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fundacioncalma.tickets.backend.seguridad.service.AuditoriaUsuarioService;
import com.fundacioncalma.tickets.backend.usuario.dto.ProvisionamientoCuentasDTO;
import com.fundacioncalma.tickets.backend.usuario.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProvisionamientoCuentasService {

    private final ProvisionamientoColaStore colaStore;
    private final UsuarioAdminService usuarioAdminService;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaUsuarioService auditoriaUsuarioService;

    @Transactional(readOnly = true)
    public ProvisionamientoCuentasDTO estado() {
        return colaStore.estado();
    }

    @Transactional
    public ProvisionamientoCuentasDTO actualizarAutomatico(boolean automatico, Authentication actor, String ip) {
        colaStore.actualizarAutomatico(automatico);
        auditoriaUsuarioService.registrar(
                usuarioRepository.findByCorreoCorporativo(actor.getName()).orElse(null),
                null,
                "CAMBIO_AUTO_PROVISIONAMIENTO",
                "Aprovisionamiento automático: " + (automatico ? "habilitado" : "deshabilitado"),
                ip);
        return colaStore.estado();
    }

    @Transactional
    public long encolarElegibles(Authentication actor, String ip) {
        long encolados = colaStore.encolarElegibles();
        auditoriaUsuarioService.registrar(
                usuarioRepository.findByCorreoCorporativo(actor.getName()).orElse(null),
                null,
                "ENCOLAR_CUENTAS_RRHH",
                "Cuentas pendientes encoladas: " + encolados,
                ip);
        return encolados;
    }

    @Transactional
    public void encolarSiAutomatico(UUID empleadoId) {
        colaStore.encolarSiAutomatico(empleadoId);
    }

    @Scheduled(fixedDelayString = "${rrhh.provisionamiento.intervalo-ms:1000}")
    public void procesarSiguiente() {
        Optional<ProvisionamientoColaStore.ElementoCola> siguiente = colaStore.reclamarSiguiente();
        if (siguiente.isEmpty()) {
            return;
        }

        ProvisionamientoColaStore.ElementoCola elemento = siguiente.get();
        try {
            boolean creada = usuarioAdminService.crearAutomaticamente(elemento.empleadoId());
            colaStore.marcarCompletado(elemento.id(), creada);
        } catch (RuntimeException e) {
            colaStore.marcarFallido(elemento.id(), e.getMessage());
            log.error("No se pudo aprovisionar una cuenta para el empleado {}", elemento.empleadoId(), e);
        }
    }
}
