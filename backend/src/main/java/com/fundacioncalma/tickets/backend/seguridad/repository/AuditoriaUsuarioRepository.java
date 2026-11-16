package com.fundacioncalma.tickets.backend.seguridad.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.fundacioncalma.tickets.backend.seguridad.model.AuditoriaUsuario;

@Repository
public interface AuditoriaUsuarioRepository extends JpaRepository<AuditoriaUsuario, UUID> {

    @Query("SELECT a FROM AuditoriaUsuario a " +
           "LEFT JOIN FETCH a.actor ac LEFT JOIN FETCH ac.empleado " +
           "LEFT JOIN FETCH a.afectado af LEFT JOIN FETCH af.empleado " +
           "ORDER BY a.fechaHora DESC")
    List<AuditoriaUsuario> findRecientes(Pageable pageable);
}
