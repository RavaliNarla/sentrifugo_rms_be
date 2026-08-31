package com.bob.candidateportal.model;

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
public class JobEligibilityValidationRequest {
    @NotNull(message = "Candidate ID is required")
    private UUID candidateId;
    
    @NotNull(message = "Position ID is required")
    private UUID positionId;
}
