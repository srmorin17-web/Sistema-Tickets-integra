package com.fundacioncalma.tickets.backend.seguridad.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fundacioncalma.tickets.backend.seguridad.dto.AuditoriaDTO;
import com.fundacioncalma.tickets.backend.seguridad.model.AuditoriaUsuario;
import com.fundacioncalma.tickets.backend.seguridad.repository.AuditoriaUsuarioRepository;
import com.fundacioncalma.tickets.backend.usuario.model.Usuario;

import lombok.RequiredArgsConstructor;

/** Registro y consulta de la auditoría de la gestión de cuentas. Nunca recibe ni guarda contraseñas. */
@Service
@RequiredArgsConstructor
public class AuditoriaUsuarioService {

    private static final int LIMITE_POR_DEFECTO = 200;
    private static final int LIMITE_MAXIMO = 500;

    private final AuditoriaUsuarioRepository repository;

    /** Se une a la transacción de quien llama: la acción y su registro se guardan juntos o no se guarda ninguno. */
    public void registrar(Usuario actor, Usuario afectado, String accion, String detalle, String ip) {
        repository.save(AuditoriaUsuario.builder()
                .actor(actor)
                .afectado(afectado)
                .accion(accion)
                .detalle(detalle == null ? null : (detalle.length() > 500 ? detalle.substring(0, 500) : detalle))
                .ipOrigen(ip == null ? null : (ip.length() > 50 ? ip.substring(0, 50) : ip))
                .fechaHora(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public List<AuditoriaDTO.GestionUsuario> listar(Integer limite) {
        int tope = (limite == null || limite < 1) ? LIMITE_POR_DEFECTO : Math.min(limite, LIMITE_MAXIMO);
        return repository.findRecientes(PageRequest.of(0, tope)).stream()
                .map(a -> new AuditoriaDTO.GestionUsuario(
                        a.getId(),
                        correo(a.getActor()), correo(a.getAfectado()),
                        a.getAccion(), a.getDetalle(), a.getIpOrigen(), a.getFechaHora()))
                .toList();
    }

    private String correo(Usuario u) {
        return u != null && u.getEmpleado() != null ? u.getEmpleado().getCorreoCorporativo() : null;
    }
}
