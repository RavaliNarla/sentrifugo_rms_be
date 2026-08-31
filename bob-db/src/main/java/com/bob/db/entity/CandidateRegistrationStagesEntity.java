package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "candidate_registration_stages", schema = "candidate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE candidate.candidate_registration_stages SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CandidateRegistrationStagesEntity extends BaseEntity<UUID> {

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "registration_step")
    private Short registrationStep = 1;

    @Column(name = "is_email_verified")
    @Builder.Default
    private Boolean isEmailVerified = false;

    @Column(name = "verification_token", length = 255)
    private String verificationToken;

    @Column(name = "token_expiry")
    private LocalDateTime tokenExpiry;

    @Column(name = "otp_code", length = 10)
    private String otpCode;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @Column(name = "is_otp_verified")
    @Builder.Default
    private Boolean isOtpVerified = false;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "otp_attempts",columnDefinition = "integer DEFAULT 5")
    private Integer otpAttempts;

    @Column(name = "privacy_notice_accepted", nullable = false)
    @Builder.Default
    private Boolean privacyNoticeAccepted = false;
}
