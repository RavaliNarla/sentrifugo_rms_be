package com.bob.db.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "candidate_active_sessions", schema = "hr")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateActiveSessionEntity extends BaseEntity<UUID> {

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    @Column(name = "refresh_token_jti", nullable = false)
    private String refreshTokenJti;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    @Column(name = "last_activity", nullable = false)
    private LocalDateTime lastActivity;

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;
}
