package com.sentrifugo.rms.masterportal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private UUID id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Role is required")
    private String role;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /** System-generated on create (EMP0001…). Read-only for clients. */
    private String employeeId;

    /**
     * Plain password — required when creating a user; never returned from the API.
     * Leave blank/null on update to keep the existing password.
     */
    private String password;
}
