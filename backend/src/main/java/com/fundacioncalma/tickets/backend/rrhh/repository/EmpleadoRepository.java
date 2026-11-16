package com.fundacioncalma.tickets.backend.rrhh.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import com.fundacioncalma.tickets.backend.rrhh.model.Empleado;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, UUID> {

    /** Proyección de solo lectura con el cargo, que la entidad Empleado no mapea. */
    interface EmpleadoResumen {
        UUID getId();
        String getNombres();
        String getApellidos();
        String getCorreoCorporativo();
        String getCargo();
    }

    Optional<Empleado> findByCorreoCorporativo(String correoCorporativo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Empleado e WHERE e.id = :id")
    Optional<Empleado> findByIdForUpdate(@Param("id") UUID id);

    /** Correo del líder jerárquico (columna lider_id de empleado.empleado), si el empleado tiene uno. */
    @Query(value = "SELECT l.correo_corporativo FROM empleado.empleado e " +
                   "JOIN empleado.empleado l ON l.id = e.lider_id WHERE e.id = :empleadoId",
           nativeQuery = true)
    Optional<String> findCorreoLiderByEmpleadoId(@Param("empleadoId") UUID empleadoId);

    @Query(value = "SELECT e.id AS id, e.nombres AS nombres, e.apellidos AS apellidos, " +
                   "e.correo_corporativo AS \"correoCorporativo\", e.cargo AS cargo " +
                   "FROM empleado.empleado e WHERE e.id = :id",
           nativeQuery = true)
    Optional<EmpleadoResumen> findResumenById(@Param("id") UUID id);

    /** Búsqueda por nombre, apellido o correo (sin distinguir mayúsculas), ordenada alfabéticamente. */
    @Query(value = "SELECT e.id AS id, e.nombres AS nombres, e.apellidos AS apellidos, " +
                   "e.correo_corporativo AS \"correoCorporativo\", e.cargo AS cargo " +
                   "FROM empleado.empleado e " +
                   "WHERE LOWER(e.nombres || ' ' || e.apellidos) LIKE LOWER(CONCAT('%', :texto, '%')) " +
                   "   OR LOWER(e.correo_corporativo) LIKE LOWER(CONCAT('%', :texto, '%')) " +
                   "ORDER BY e.apellidos, e.nombres LIMIT :limite",
           nativeQuery = true)
    List<EmpleadoResumen> buscar(@Param("texto") String texto, @Param("limite") int limite);
}
