package com.bob.db.repository;

import com.bob.db.entity.CandidateActiveSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateActiveSessionRepository extends JpaRepository<CandidateActiveSessionEntity, UUID> {

    // Count active sessions for an email (expiry_time > current time)
    @Query("SELECT COUNT(s) FROM CandidateActiveSessionEntity s WHERE s.userEmail = :email AND s.expiryTime > :currentTime")
    long countActiveSessionsByEmail(@Param("email") String email, @Param("currentTime") LocalDateTime currentTime);

    // Find all active sessions for an email
    @Query("SELECT s FROM CandidateActiveSessionEntity s WHERE s.userEmail = :email AND s.expiryTime > :currentTime ORDER BY s.loginTime ASC")
    List<CandidateActiveSessionEntity> findActiveSessionsByEmail(@Param("email") String email, @Param("currentTime") LocalDateTime currentTime);

    // Delete expired sessions (cleanup job)
    @Modifying
    @Transactional
    @Query("DELETE FROM CandidateActiveSessionEntity s WHERE s.expiryTime < :currentTime")
    int deleteExpiredSessions(@Param("currentTime") LocalDateTime currentTime);

    // Find by session ID
    Optional<CandidateActiveSessionEntity> findBySessionId(String sessionId);

    // Delete by session ID (for logout)
    @Modifying
    @Transactional
    void deleteBySessionId(String sessionId);

    // Delete by refresh token JTI
    @Modifying
    @Transactional
    void deleteByRefreshTokenJti(String refreshTokenJti);
}
