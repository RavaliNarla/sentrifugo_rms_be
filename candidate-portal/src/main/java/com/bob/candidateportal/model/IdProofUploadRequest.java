package com.bob.candidateportal.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdProofUploadRequest {

    @NotNull(message = "Document ID is required")
    private UUID documentId;

    @NotBlank(message = "Document number is required")
    private String documentNumber;

//    @NotNull(message = "isValidationPending is required")
    private Boolean isValidationPending;

    private List<String> pendingChecks;
}
