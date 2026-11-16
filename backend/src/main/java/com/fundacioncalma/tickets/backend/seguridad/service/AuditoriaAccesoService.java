package com.fundacioncalma.tickets.backend.seguridad.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fundacioncalma.tickets.backend.seguridad.dto.AuditoriaDTO;
import com.fundacioncalma.tickets.backend.seguridad.model.IntentoAcceso;
import com.fundacioncalma.tickets.backend.seguridad.model.RegistroAcceso;
import com.fundacioncalma.tickets.backend.seguridad.repository.IntentoAccesoRepository;
import com.fundacioncalma.tickets.backend.seguridad.repository.RegistroAccesoRepository;
import com.fundacioncalma.tickets.backend.usuario.model.Usuario;
import com.fundacioncalma.tickets.backend.usuario.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Registro y consulta de la auditoría de seguridad (Acta de Refinamiento, sección 5):
 * inicios de sesión e intentos de acceso no permitidos.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaAccesoService {

    private static final int LIMITE_POR_DEFECTO = 200;
    private static final int LIMITE_MAXIMO = 500;

    private final RegistroAccesoRepository registroAccesoRepository;
    private final IntentoAccesoRepository intentoAccesoRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Se guarda en una transacción propia para que el registro de un inicio de sesión fallido
     * no dependa de que la operación que lo invoca termine bien.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarLogin(String correo, String ip, String resultado) {
        Usuario usuario = buscarUsuario(correo);
        registroAccesoRepository.save(RegistroAcceso.builder()
                .usuario(usuario)
                .correoIntentado(truncar(correo == null ? "" : correo, 255))
                .fechaHora(LocalDateTime.now())
                .ipOrigen(truncar(ip, 50))
                .resultado(resultado)
                .build());
    }

    /** correo puede ser nulo si quien intentó el acceso no estaba autenticado. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarIntentoNoPermitido(String correo, String ruta, String metodo, String ip, String resultado) {
        intentoAccesoRepository.save(IntentoAcceso.builder()
                .usuario(buscarUsuario(correo))
                .rutaIntentada(truncar(ruta, 500))
                .metodoHttp(truncar(metodo, 20))
                .fechaIntento(LocalDateTime.now())
                .ipOrigen(truncar(ip, 50))
                .resultado(truncar(resultado, 50))
                .build());
    }

    @Transactional(readOnly = true)
    public List<AuditoriaDTO.Acceso> listarAccesos(Integer limite) {
        return registroAccesoRepository.findRecientes(PageRequest.of(0, limitar(limite))).stream()
                .map(r -> new AuditoriaDTO.Acceso(
                        r.getId(), r.getCorreoIntentado(), r.getResultado(), r.getIpOrigen(), r.getFechaHora()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditoriaDTO.Intento> listarIntentos(Integer limite) {
        return intentoAccesoRepository.findRecientes(PageRequest.of(0, limitar(limite))).stream()
                .map(i -> new AuditoriaDTO.Intento(
                        i.getId(),
                        i.getUsuario() != null ? i.getUsuario().getEmpleado().getCorreoCorporativo() : null,
                        i.getRutaIntentada(), i.getMetodoHttp(), i.getResultado(), i.getIpOrigen(),
                        i.getFechaIntento()))
                .toList();
    }

    private Usuario buscarUsuario(String correo) {
        if (correo == null || correo.isBlank()) {
            return null;
        }
        return usuarioRepository.findByCorreoCorporativo(correo).orElse(null);
    }

    private int limitar(Integer limite) {
        if (limite == null || limite < 1) {
            return LIMITE_POR_DEFECTO;
        }
        return Math.min(limite, LIMITE_MAXIMO);
    }

    private String truncar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() > max ? valor.substring(0, max) : valor;
    }
}
