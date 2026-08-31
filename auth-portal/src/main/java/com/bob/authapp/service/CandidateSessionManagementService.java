package com.bob.authapp.service;

import com.bob.commonutil.exception.MaxSessionsExceededException;
import com.bob.commonutil.util.AppConstants;
import com.bob.db.entity.CandidateActiveSessionEntity;
import com.bob.db.repository.CandidateActiveSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class CandidateSessionManagementService {

    @Autowired
    private CandidateActiveSessionRepository sessionRepository;

    /**
     * Validates if the email has not exceeded the maximum concurrent session limit
     * @param email The candidate email to check
     *
     */
    public void validateSessionLimit(String email){
        long activeSessionCount = sessionRepository.countActiveSessionsByEmail(
                email.toLowerCase(),
                LocalDateTime.now()
        );

        log.info("Active session count for email {}: {}", email, activeSessionCount);

        if (activeSessionCount >= AppConstants.MAX_CONCURRENT_SESSIONS) {
            log.warn("Maximum concurrent sessions ({}) exceeded for email: {}",
                    AppConstants.MAX_CONCURRENT_SESSIONS, email);
            throw new MaxSessionsExceededException(email, AppConstants.MAX_CONCURRENT_SESSIONS);
        }
    }

    /**
     * Creates a new session record after successful login
     * @param email Candidate email
     * @param refreshToken Refresh token (UUID string)
     * @param request HTTP request to extract IP and user agent
     */
    @Transactional
    public void createSession(String email, String refreshToken, HttpServletRequest request) {
        try {
            // Extract IP address
            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty()) {
                ipAddress = request.getRemoteAddr();
            }

            // Extract User Agent
            String userAgent = request.getHeader("User-Agent");

            // Create session entity
            CandidateActiveSessionEntity session = new CandidateActiveSessionEntity();
            session.setUserEmail(email.toLowerCase());
            session.setSessionId(UUID.randomUUID().toString());
            session.setRefreshTokenJti(refreshToken); // Store the refresh token UUID directly
            session.setIpAddress(ipAddress);
            session.setUserAgent(userAgent);
            session.setLoginTime(LocalDateTime.now());
            session.setLastActivity(LocalDateTime.now());
            session.setExpiryTime(LocalDateTime.now().plusMinutes(5)); // Match refresh token expiry (30 days from cookies)

            sessionRepository.save(session);

            log.info("Created new session for email: {} with session ID: {}", email, session.getSessionId());
        } catch (Exception e) {
            log.error("Error creating session for email: {}", email, e);
            // Don't throw exception - allow login to proceed even if session tracking fails
        }
    }

    /**
     * Deletes a session by session ID (for logout)
     * @param sessionId The session ID to delete
     */
    @Transactional
    public void deleteSession(String sessionId) {
        try {
            sessionRepository.deleteBySessionId(sessionId);
            log.info("Deleted session with ID: {}", sessionId);
        } catch (Exception e) {
            log.error("Error deleting session with ID: {}", sessionId, e);
        }
    }

    /**
     * Deletes a session by refresh token JTI
     * @param refreshTokenJti The refresh token JTI
     */
    @Transactional
    public void deleteSessionByRefreshToken(String refreshTokenJti) {
        try {
            sessionRepository.deleteByRefreshTokenJti(refreshTokenJti);
            log.info("Deleted session with refresh token JTI: {}", refreshTokenJti);
        } catch (Exception e) {
            log.error("Error deleting session with refresh token JTI: {}", refreshTokenJti, e);
        }
    }

    /**
     * Cleans up expired sessions (called by scheduled task)
     * @return Number of sessions deleted
     */
    @Transactional
    public int cleanupExpiredSessions() {
        try {
            int deletedCount = sessionRepository.deleteExpiredSessions(LocalDateTime.now());
            log.info("Cleaned up {} expired candidate sessions", deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("Error cleaning up expired sessions", e);
            return 0;
        }
    }

    /**
     * Gets all active sessions for an email
     * @param email The candidate email
     * @return List of active sessions
     */
    public List<CandidateActiveSessionEntity> getActiveSessions(String email) {
        return sessionRepository.findActiveSessionsByEmail(
                email.toLowerCase(),
                LocalDateTime.now()
        );
    }
}
