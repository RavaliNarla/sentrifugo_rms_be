package com.bob.jobportal.model;

import com.bob.db.enums.DocumentZonalVerificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZonalDocumentVerificationRequest {
    
    @NotNull(message = "Candidate Document Id is required")
    private UUID candidateDocumentId;

    @NotNull(message = "Candidate Id is required")
    private UUID candidateId;

    @NotNull(message = "Application Id is required")
    private UUID applicationId;

    @NotNull(message = "Document status is required")
    private DocumentZonalVerificationStatus zonalHrDocStatus;

    private String zonalHrDocComments;
}
