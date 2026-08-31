package com.bob.jobportal.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AddAdditionalRequiredDocumentRequest {

    @NotNull(message = "Application Id is required")
    private UUID applicationId;

    @NotNull(message = "Candidate Id is required")
    private UUID candidateId;

    @NotBlank(message = "Document name is required")
    private String displayName;
}

