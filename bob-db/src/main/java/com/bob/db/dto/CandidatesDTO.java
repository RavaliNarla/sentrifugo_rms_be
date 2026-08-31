package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.io.Serializable;

@Data
public class CandidatesDTO extends BaseDTO implements Serializable {
    @JsonProperty("candidateId")
    private UUID id;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be exactly 10 digits")
    private String mobileNumber;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @JsonIgnore  // Exclude password hash from JSON serialization for security
    private String passwordHash;

    private Boolean isEmailVerified = false;

    @JsonIgnore
    private String refreshToken;

    @JsonIgnore
    private LocalDateTime lastLogin;

    @JsonIgnore
    private Boolean candidateTempLock = false;

    @JsonIgnore
    private String candidateReasonLocked;

    @JsonIgnore
    private LocalDate candidateTempLockedDate;

    private Boolean isProfileCompleted = false;

    private Short currentStep = 1;

    @JsonIgnore
    private String otpCode;

    @JsonIgnore
    private LocalDateTime otpExpiry;

    private LocalDate dateOfBirth;

    private Boolean finalDeclarationAccepted = false;

    private LocalDate passwordExpiryDate;

    private LocalDate passwordLastChangedAt;

    private String registrationNo;

    private Boolean privacyNoticeAccepted = false;
}
