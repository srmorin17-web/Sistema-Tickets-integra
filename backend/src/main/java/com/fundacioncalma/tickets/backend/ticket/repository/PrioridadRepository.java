package com.fundacioncalma.tickets.backend.ticket.repository;

import com.fundacioncalma.tickets.backend.ticket.model.Prioridad; // <-- IMPORT CRUCIAL
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

public interface PrioridadRepository extends JpaRepository<Prioridad, UUID> {}
