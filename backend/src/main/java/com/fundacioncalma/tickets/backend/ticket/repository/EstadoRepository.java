package com.fundacioncalma.tickets.backend.ticket.repository;

import com.fundacioncalma.tickets.backend.ticket.model.Estado; // <-- IMPORT CRUCIAL
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

public interface EstadoRepository extends JpaRepository<Estado, UUID> {
    Optional<Estado> findByNombre(String nombre);
}