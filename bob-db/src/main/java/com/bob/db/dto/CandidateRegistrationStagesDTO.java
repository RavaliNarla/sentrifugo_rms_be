package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CandidateRegistrationStagesDTO extends BaseDTO implements Serializable {

    @JsonProperty("candidateRegistrationId")
    private UUID id;

    private String email;

    private String fullName;

    private String mobileNumber;

    private String passwordHash;

    private Short registrationStep;

    private Boolean isEmailVerified;

    private String verificationToken;

    private LocalDateTime tokenExpiry;

    private String otpCode;

    private LocalDateTime otpExpiry;

    private Boolean isOtpVerified;

    private LocalDate dateOfBirth;

    @NotNull(message = "Privacy Notice consent is required.")
    @AssertTrue(message = "You must accept the Privacy Notice to continue.")
    private Boolean privacyNoticeAccepted = false;
}
