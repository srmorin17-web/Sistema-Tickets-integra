package com.fundacioncalma.tickets.backend.seguridad.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fundacioncalma.tickets.backend.seguridad.model.RegistroAcceso;

@Repository
public interface RegistroAccesoRepository extends JpaRepository<RegistroAcceso, UUID> {

    @Query("SELECT r FROM RegistroAcceso r ORDER BY r.fechaHora DESC")
    List<RegistroAcceso> findRecientes(Pageable pageable);

    /** Fecha del último inicio de sesión exitoso del correo; nulo si nunca entró. */
    @Query("SELECT MAX(r.fechaHora) FROM RegistroAcceso r WHERE LOWER(r.correoIntentado) = LOWER(:correo) AND r.resultado = 'EXITOSO'")
    LocalDateTime findUltimoExitoso(@Param("correo") String correo);

    /** Intentos fallidos del correo posteriores a la fecha indicada (base del bloqueo temporal). */
    @Query("SELECT COUNT(r) FROM RegistroAcceso r WHERE LOWER(r.correoIntentado) = LOWER(:correo) AND r.resultado = 'FALLIDO' AND r.fechaHora > :desde")
    long contarFallidosDesde(@Param("correo") String correo, @Param("desde") LocalDateTime desde);

    /** Último acceso exitoso de cada correo: [correo en minúsculas, fecha]. */
    @Query("SELECT LOWER(r.correoIntentado), MAX(r.fechaHora) FROM RegistroAcceso r WHERE r.resultado = 'EXITOSO' GROUP BY LOWER(r.correoIntentado)")
    List<Object[]> findUltimosAccesos();
}
