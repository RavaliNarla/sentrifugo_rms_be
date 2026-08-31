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
public class WorkExpUploadRequest {

    @NotNull(message = "Document ID is required")
    private UUID documentId;

    @NotBlank(message = "Organization name is required")
    private String organization;

    private String role;

    private String postHeld;

    @NotBlank(message = "From date is required")
    private String fromDate; // Expected format: dd/mm/yyyy

    private String toDate; // Expected format: dd/mm/yyyy (not required if isPresentlyWorking is true)

    @NotNull(message = "isPresentlyWorking flag is required")
    private Boolean isPresentlyWorking;
}
