package com.fundacioncalma.tickets.backend.usuario.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import com.fundacioncalma.tickets.backend.usuario.model.RefreshSession;

import jakarta.persistence.LockModeType;

@Repository
public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM RefreshSession s JOIN FETCH s.usuario u JOIN FETCH u.rol JOIN FETCH u.empleado WHERE s.refreshTokenHash = :hash")
    Optional<RefreshSession> findByRefreshTokenHashForUpdate(@Param("hash") String hash);

    @Modifying
    @Query("UPDATE RefreshSession s SET s.revoked = true WHERE s.familyId = :familyId")
    int revokeFamily(@Param("familyId") UUID familyId);

    @Modifying
    @Query("UPDATE RefreshSession s SET s.revoked = true WHERE s.usuario.id = :userId")
    int revokeAllForUser(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM RefreshSession s WHERE s.absoluteExpiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
