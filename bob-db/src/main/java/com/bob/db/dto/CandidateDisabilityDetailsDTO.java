package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class CandidateDisabilityDetailsDTO extends BaseDTO implements Serializable {

    @JsonProperty("candidateDisabilityId")
    private UUID id;

    @NotNull(message = "Candidate ID is required")
    private UUID candidateId;

    @NotNull(message = "Disability category is required")
    private UUID disabilityCategoryId;

    @Min(value = 0, message = "Disability percentage must be at least 0")
    private Short disabilityPercentage;

}

