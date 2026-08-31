package com.bob.authapp.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank(message = "Credentials are required")
    private String credentials;
}
