package com.fundacioncalma.tickets.backend.seguridad.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.fundacioncalma.tickets.backend.seguridad.model.IntentoAcceso;

@Repository
public interface IntentoAccesoRepository extends JpaRepository<IntentoAcceso, UUID> {

    @Query("SELECT i FROM IntentoAcceso i LEFT JOIN FETCH i.usuario u LEFT JOIN FETCH u.empleado " +
           "ORDER BY i.fechaIntento DESC")
    List<IntentoAcceso> findRecientes(Pageable pageable);
}
