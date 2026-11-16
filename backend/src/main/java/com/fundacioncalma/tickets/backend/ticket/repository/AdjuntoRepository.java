package com.fundacioncalma.tickets.backend.ticket.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fundacioncalma.tickets.backend.ticket.model.Adjunto;

@Repository
public interface AdjuntoRepository extends JpaRepository<Adjunto, UUID> {

    @Query("SELECT a FROM Adjunto a LEFT JOIN FETCH a.subidoPor u LEFT JOIN FETCH u.empleado " +
           "WHERE a.ticket.id = :ticketId ORDER BY a.fechaSubida ASC")
    List<Adjunto> findByTicketId(@Param("ticketId") UUID ticketId);

    /** Adjuntos vigentes (no purgados), que cuentan para el máximo por ticket. */
    @Query("SELECT COUNT(a) FROM Adjunto a WHERE a.ticket.id = :ticketId AND a.purgado = false")
    long contarVigentes(@Param("ticketId") UUID ticketId);

    /** Adjuntos de tickets cerrados antes de la fecha límite (política de retención). */
    @Query("SELECT a FROM Adjunto a WHERE a.purgado = false AND a.ticket.fechaCierre IS NOT NULL " +
           "AND a.ticket.fechaCierre < :limite")
    List<Adjunto> findParaPurgar(@Param("limite") LocalDateTime limite);
}
