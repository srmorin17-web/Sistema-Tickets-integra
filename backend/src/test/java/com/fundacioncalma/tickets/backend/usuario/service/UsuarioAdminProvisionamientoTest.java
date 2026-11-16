package com.fundacioncalma.tickets.backend.usuario.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fundacioncalma.tickets.backend.rrhh.model.Empleado;
import com.fundacioncalma.tickets.backend.rrhh.repository.EmpleadoRepository;
import com.fundacioncalma.tickets.backend.rrhh.service.RrhhClientService;
import com.fundacioncalma.tickets.backend.seguridad.service.AuditoriaUsuarioService;
import com.fundacioncalma.tickets.backend.seguridad.repository.RegistroAccesoRepository;
import com.fundacioncalma.tickets.backend.usuario.model.Rol;
import com.fundacioncalma.tickets.backend.usuario.model.Usuario;
import com.fundacioncalma.tickets.backend.usuario.repository.RefreshSessionRepository;
import com.fundacioncalma.tickets.backend.usuario.repository.RolRepository;
import com.fundacioncalma.tickets.backend.usuario.repository.UsuarioRepository;

class UsuarioAdminProvisionamientoTest {

    @Test
    void creaCuentaPendienteConRolEmpleadoYEnviaActivacion() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        RolRepository roles = mock(RolRepository.class);
        EmpleadoRepository empleados = mock(EmpleadoRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        CuentaActivacionService activacion = mock(CuentaActivacionService.class);
        UUID empleadoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Empleado empleado = Empleado.builder()
                .id(empleadoId)
                .correoCorporativo("persona@example.org")
                .estadoLaboral("ACTIVO")
                .build();
        Rol rol = Rol.builder().id(UUID.randomUUID()).nombre("Empleado").build();
        when(empleados.findByIdForUpdate(empleadoId)).thenReturn(Optional.of(empleado));
        when(usuarios.existeParaEmpleado(empleadoId)).thenReturn(false);
        when(roles.findByNombre("Empleado")).thenReturn(Optional.of(rol));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(usuarioId);
            return usuario;
        });

        UsuarioAdminService service = crearServicio(usuarios, roles, empleados, passwordEncoder, activacion);

        assertTrue(service.crearAutomaticamente(empleadoId));
        verify(activacion).enviarEnlace(usuarioId);
        verify(usuarios).saveAndFlush(any(Usuario.class));
    }

    @Test
    void noDuplicaUnaCuentaExistente() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        EmpleadoRepository empleados = mock(EmpleadoRepository.class);
        UUID empleadoId = UUID.randomUUID();
        when(empleados.findByIdForUpdate(empleadoId))
                .thenReturn(Optional.of(Empleado.builder().id(empleadoId).estadoLaboral("ACTIVO").build()));
        when(usuarios.existeParaEmpleado(empleadoId)).thenReturn(true);
        CuentaActivacionService activacion = mock(CuentaActivacionService.class);

        UsuarioAdminService service = crearServicio(
                usuarios, mock(RolRepository.class), empleados, mock(PasswordEncoder.class), activacion);

        assertFalse(service.crearAutomaticamente(empleadoId));
        verify(usuarios, never()).saveAndFlush(any(Usuario.class));
        verify(activacion, never()).enviarEnlace(any());
    }

    private UsuarioAdminService crearServicio(
            UsuarioRepository usuarios,
            RolRepository roles,
            EmpleadoRepository empleados,
            PasswordEncoder passwordEncoder,
            CuentaActivacionService activacion) {
        return new UsuarioAdminService(
                usuarios,
                roles,
                empleados,
                mock(RrhhClientService.class),
                passwordEncoder,
                mock(AuditoriaUsuarioService.class),
                mock(RegistroAccesoRepository.class),
                mock(RefreshSessionRepository.class),
                activacion);
    }
}
