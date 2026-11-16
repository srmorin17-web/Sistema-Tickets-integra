package com.fundacioncalma.tickets.backend.ticket.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fundacioncalma.tickets.backend.ticket.model.HistorialAuditoria;

@Repository
public interface HistorialAuditoriaRepository extends JpaRepository<HistorialAuditoria, UUID> {

    @Query("SELECT h FROM HistorialAuditoria h LEFT JOIN FETCH h.usuario u LEFT JOIN FETCH u.empleado " +
           "WHERE h.ticket.id = :ticketId ORDER BY h.timestamp ASC")
    List<HistorialAuditoria> findByTicketIdOrdenado(@Param("ticketId") UUID ticketId);

    @Query("SELECT COUNT(h) > 0 FROM HistorialAuditoria h WHERE h.ticket.id = :ticketId AND h.tipoAccion = :tipo")
    boolean existeAccion(@Param("ticketId") UUID ticketId, @Param("tipo") String tipo);
}
