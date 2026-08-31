package com.bob.candidateportal.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BirthdateProofUploadRequest {

    @NotNull(message = "Document ID is required")
    private UUID documentId;

    @NotBlank(message = "Full name as per certificate is required")
    private String fullNameAsPerCertificate;

    @NotBlank(message = "Date of birth is required")
    private String dateOfBirth; // Expected format: dd/mm/yyyy
}
