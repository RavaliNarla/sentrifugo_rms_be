package com.sentrifugo.rms.authportal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    /** Employee ID or email. */
    @NotBlank(message = "Username is required")
    private String username;
}
