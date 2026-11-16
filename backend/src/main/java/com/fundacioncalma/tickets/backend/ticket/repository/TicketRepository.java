package com.fundacioncalma.tickets.backend.ticket.repository;

import com.fundacioncalma.tickets.backend.ticket.model.Ticket;
import com.fundacioncalma.tickets.backend.usuario.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fundacioncalma.tickets.backend.metricas.dto.ConteoProjection;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    
    // Regla de Negocio: Asignación por menor carga (Round Robin inteligente)
    @Query("SELECT u FROM Usuario u JOIN u.rol r WHERE r.nombre = 'Técnico TI' " +
           "ORDER BY (SELECT COUNT(t) FROM Ticket t WHERE t.tecnicoAsignado = u AND t.estado.nombre IN ('Recibido', 'En revisión', 'Requiere información')) ASC")
    List<Usuario> findTecnicosPorMenorCarga();

    // Carga de cada técnico: [Usuario, tickets activos, fecha del último ticket recibido]
    @Query("SELECT u, " +
           "(SELECT COUNT(t) FROM Ticket t WHERE t.tecnicoAsignado = u AND t.estado.nombre IN ('Recibido', 'En revisión', 'Requiere información')), " +
           "(SELECT MAX(t2.fechaCreacion) FROM Ticket t2 WHERE t2.tecnicoAsignado = u) " +
           "FROM Usuario u WHERE u.rol.nombre = 'Técnico TI' AND u.activo = true")
    List<Object[]> findCargaTecnicos();

    // Supervisión: tickets activos de todos los técnicos; primero los sin asignar y luego por tiempo restante de SLA
    @Query("SELECT t FROM Ticket t WHERE t.estado.esEstadoFinal = false " +
           "ORDER BY CASE WHEN t.tecnicoAsignado IS NULL THEN 0 ELSE 1 END ASC, " +
           "CASE WHEN t.pausado = true THEN 1 ELSE 0 END ASC, t.fechaVencimientoSla ASC")
    List<Ticket> findActivosParaSupervision();

    // Tickets en estado Escalado cuya área tiene como representante al empleado indicado; los más antiguos primero
    @Query("SELECT t FROM Ticket t WHERE t.estado.nombre = 'Escalado' " +
           "AND t.areaEscalada.contactoResponsableId = :empleadoId ORDER BY t.fechaActualizacion ASC")
    List<Ticket> findEscaladosPorRepresentante(@Param("empleadoId") UUID empleadoId);

    Optional<Ticket> findByCodigo(String codigo);

    List<Ticket> findAllByOrderByFechaCreacionDesc();

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Ticket t " +
           "WHERE t.solicitante.id = :solicitanteId AND t.estado.esEstadoFinal = false")
    boolean existeTicketActivoDelSolicitante(@Param("solicitanteId") UUID solicitanteId);

    // 1. Tickets por Estado
@Query("SELECT e.nombre as nombre, COUNT(t) as cantidad FROM Ticket t JOIN t.estado e " +
       "WHERE t.fechaCreacion BETWEEN :inicio AND :fin GROUP BY e.nombre")
List<ConteoProjection> countByEstado(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

// 2. Tickets por Prioridad
@Query("SELECT p.nombre as nombre, COUNT(t) as cantidad FROM Ticket t JOIN t.prioridad p " +
       "WHERE t.fechaCreacion BETWEEN :inicio AND :fin GROUP BY p.nombre")
List<ConteoProjection> countByPrioridad(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

// 3. Tickets por Categoría
@Query("SELECT c.nombre as nombre, COUNT(t) as cantidad FROM Ticket t JOIN t.categoria c " +
       "WHERE t.fechaCreacion BETWEEN :inicio AND :fin GROUP BY c.nombre")
List<ConteoProjection> countByCategoria(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

// 4. Carga actual por Técnico (Solo tickets activos)
@Query("SELECT CONCAT(u.empleado.nombres, ' ', u.empleado.apellidos) as nombre, COUNT(t) as cantidad " +
       "FROM Ticket t JOIN t.tecnicoAsignado u " +
       "WHERE t.estado.esEstadoFinal = false GROUP BY u.id, u.empleado.nombres, u.empleado.apellidos")
List<ConteoProjection> getCargaPorTecnico();

   // 5. Estadísticas de SLA (Total cerrados vs Cumplidos)
    @Query("SELECT " +
           "SUM(CASE WHEN e.esEstadoFinal = true THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN e.esEstadoFinal = true AND t.fechaCierre <= t.fechaVencimientoSla THEN 1 ELSE 0 END) " +
           "FROM Ticket t JOIN t.estado e " +
           "WHERE t.fechaCreacion BETWEEN :inicio AND :fin")
    List<Object[]> getSlAStats(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
    
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.areaEscalada.id = :areaId AND t.estado.esEstadoFinal = false")
long countTicketsActivosPorArea(@Param("areaId") UUID areaId);

@Query(value = """
    SELECT 
        t.codigo, t.asunto,
        CONCAT(e.nombres, ' ', e.apellidos) as solicitante,
        COALESCE(CONCAT(etec.nombres, ' ', etec.apellidos), 'Sin asignar') as tecnico,
        c.nombre as categoria, p.nombre as prioridad, est.nombre as estado,
        t.fecha_creacion, t.fecha_cierre, t.fecha_vencimiento_sla
    FROM ticket.ticket t
    JOIN empleado.empleado e ON t.solicitante_id = e.id
    LEFT JOIN usuario.usuario u ON t.tecnico_asignado_id = u.id
    LEFT JOIN empleado.empleado etec ON u.id_empleado = etec.id
    JOIN ticket.categoria c ON t.categoria_id = c.id
    JOIN ticket.prioridad p ON t.prioridad_id = p.id
    JOIN ticket.estado est ON t.estado_id = est.id
    WHERE t.fecha_creacion BETWEEN :inicio AND :fin
    AND (:tecnicoId IS NULL OR t.tecnico_asignado_id = :tecnicoId)
    AND (:categoriaId IS NULL OR t.categoria_id = :categoriaId)
    ORDER BY t.fecha_creacion DESC
    """, nativeQuery = true)
List<Object[]> generarDataReporte(
        @Param("inicio") LocalDateTime inicio, 
        @Param("fin") LocalDateTime fin,
        @Param("tecnicoId") UUID tecnicoId, 
        @Param("categoriaId") UUID categoriaId);

@Query("SELECT t FROM Ticket t " +
       "WHERE t.tecnicoAsignado.id = :tecnicoId " +
       "AND t.estado.esEstadoFinal = false " +
       // Primero el menor tiempo restante de SLA; los tickets con SLA en pausa al final; la prioridad desempata
       "ORDER BY CASE WHEN t.pausado = true THEN 1 ELSE 0 END ASC, t.fechaVencimientoSla ASC, t.prioridad.slaHoras ASC")
List<Ticket> findBandejaPorTecnico(@Param("tecnicoId") UUID tecnicoId);

    // Tickets creados por un solicitante (por correo corporativo), del más reciente al más antiguo
    @Query("SELECT t FROM Ticket t WHERE LOWER(t.solicitante.correoCorporativo) = LOWER(:correo) " +
           "ORDER BY t.fechaCreacion DESC")
    List<Ticket> findBySolicitanteCorreo(@Param("correo") String correo);
}
