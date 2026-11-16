package com.fundacioncalma.tickets.backend.usuario.repository;


import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.fundacioncalma.tickets.backend.usuario.model.Usuario;

import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param; // Agrega este import

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol JOIN FETCH u.empleado WHERE u.empleado.correoCorporativo = :correo")
    Optional<Usuario> findByCorreoCorporativo(@Param("correo") String correo); // <-- Agrega @Param aquí

    @Query("SELECT new com.fundacioncalma.tickets.backend.usuario.dto.EstadoCuenta(u.activo, u.activada, u.debeCambiarContrasena, r.nombre) " +
           "FROM Usuario u JOIN u.rol r WHERE u.empleado.correoCorporativo = :correo")
    Optional<com.fundacioncalma.tickets.backend.usuario.dto.EstadoCuenta> findEstadoCuenta(@Param("correo") String correo);

    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol JOIN FETCH u.empleado e ORDER BY e.apellidos, e.nombres")
    java.util.List<Usuario> findAllConDatos();

    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol JOIN FETCH u.empleado WHERE u.id = :id")
    Optional<Usuario> findByIdConDatos(@Param("id") UUID id);

    @Query("SELECT COUNT(u) > 0 FROM Usuario u WHERE u.empleado.id = :empleadoId")
    boolean existeParaEmpleado(@Param("empleadoId") UUID empleadoId);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.rol.nombre = 'Superadmin' AND u.activo = true")
    long contarSuperadminsActivos();
}
