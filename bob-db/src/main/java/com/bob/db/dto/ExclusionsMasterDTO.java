package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class ExclusionsMasterDTO extends BaseDTO implements Serializable {

    @JsonProperty("exclusionId")
    private UUID id;

    @NotBlank(message = "Exclusion type is required")
    private String exclusionType;

    @NotBlank(message = "Exclusion value is required")
    private String exclusionValue;

}

