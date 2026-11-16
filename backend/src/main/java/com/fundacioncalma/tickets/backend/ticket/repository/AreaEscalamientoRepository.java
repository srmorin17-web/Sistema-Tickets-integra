package com.fundacioncalma.tickets.backend.ticket.repository;

import com.fundacioncalma.tickets.backend.ticket.model.AreaEscalamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AreaEscalamientoRepository extends JpaRepository<AreaEscalamiento, UUID> {
    boolean existsByNombre(String nombre);

    /** Áreas cuyo representante es el empleado indicado (una persona puede representar a más de un área). */
    List<AreaEscalamiento> findByContactoResponsableId(UUID contactoResponsableId);
}
