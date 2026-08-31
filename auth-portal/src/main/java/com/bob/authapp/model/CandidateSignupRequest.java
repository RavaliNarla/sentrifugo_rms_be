package com.bob.authapp.model;

import com.bob.commonutil.model.CaptchaRequestModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CandidateSignupRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 255, message = "Full name must be between 2 and 255 characters")
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be exactly 10 digits")
    private String mobileNumber;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Credentials are required")
    private String credentials;

    @JsonProperty("captcha")
    private CaptchaRequestModel captcha;

    @AssertTrue(message = "You must accept the Privacy Notice to continue.")
    @NotNull(message = "Privacy Notice consent is required.")
    private Boolean privacyNoticeAccepted = false;
}
