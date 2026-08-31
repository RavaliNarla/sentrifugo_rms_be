package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class JobPositionExclusionsDTO extends BaseDTO implements Serializable {

    @JsonProperty("jobPositionExclusionId")
    private UUID id;

    private UUID positionId;

    @NotNull(message = "Exclusion id is required")
    private UUID exclusionId;

    private Boolean isExcluded = false;

}

