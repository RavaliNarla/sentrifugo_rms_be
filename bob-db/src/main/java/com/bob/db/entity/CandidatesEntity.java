package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "candidates", schema = "candidate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE candidate.candidates SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CandidatesEntity extends BaseEntity<UUID> {

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "is_email_verified", nullable = false)
    @Builder.Default
    private Boolean isEmailVerified = false;

    @Column(name = "refresh_token", length = 255)
    private String refreshToken;

    @LastModifiedDate
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "candidate_temp_lock", nullable = false)
    @Builder.Default
    private Boolean candidateTempLock = false;

    @Column(name = "candidate_reason_locked", length = 255)
    private String candidateReasonLocked;

    @Column(name = "candidate_temp_locked_date")
    private LocalDate candidateTempLockedDate;

    @Column(name = "is_profile_completed", nullable = false)
    @Builder.Default
    private Boolean isProfileCompleted = false;

    @Builder.Default
    @Column(name = "current_step", nullable = false)
    private Short currentStep = 1;

    @Column(name = "otp_code", length = 10)
    private String otpCode;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "final_declaration_accepted", nullable = false)
    @Builder.Default
    private Boolean finalDeclarationAccepted = false;

    @Column(name = "otp_attempts",columnDefinition = "integer DEFAULT 5")
    private Integer otpAttempts;

    @Column(name = "password_expiry_date")
    private LocalDateTime passwordExpiryDate;

    @Column(name = "password_last_changed_at")
    private LocalDateTime passwordLastChangedAt;

    @Column(name = "registration_no",insertable = false, updatable = false )
    private String registrationNo;

    @Column(name = "privacy_notice_accepted", nullable = false)
    @Builder.Default
    private Boolean privacyNoticeAccepted = false;
}
