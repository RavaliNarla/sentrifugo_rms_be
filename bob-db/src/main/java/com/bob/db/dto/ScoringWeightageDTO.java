package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ScoringWeightageDTO extends BaseDTO implements Serializable {

    @JsonProperty("scoringWeightageId")
    private UUID id;

    private BigDecimal educationScore;

    private BigDecimal experienceScore;

    private BigDecimal educationSimilarity;

    private BigDecimal experienceSimilarity;

}