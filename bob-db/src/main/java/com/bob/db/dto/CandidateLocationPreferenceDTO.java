package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CandidateLocationPreferenceDTO extends BaseDTO implements Serializable {
    @JsonProperty("candidateLocationPreferenceId")
    private UUID id;

    @NotNull(message = "Candidate ID is required")
    private UUID candidateId;

    @NotNull(message = "Position ID is required")
    private UUID positionId;

    private UUID countryId;

    private UUID statePreference1;

    private UUID cityPreference1;

    private UUID locationPreference1;

    private UUID statePreference2;

    private UUID cityPreference2;

    private UUID locationPreference2;

    private UUID statePreference3;

    private UUID cityPreference3;

    private UUID locationPreference3;

    private BigDecimal expectedCtc;

    @NotNull(message = "Interview center is required")
    private UUID interviewCenter;

    private UUID localLanguageId;

    private Boolean isLocalLanguageStudied = false;

    private JsonNode dynamicFormData;
}
