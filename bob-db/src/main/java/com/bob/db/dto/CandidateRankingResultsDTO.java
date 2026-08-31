package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CandidateRankingResultsDTO extends BaseDTO implements Serializable {

    @JsonProperty("candidateRankingResultId")
    private UUID id;

    private UUID candidateId;

    private UUID applicationId;

    private BigDecimal finalScore;

    private BigDecimal educationScore;

    private BigDecimal experienceScore;

    private BigDecimal educationSimilarity;

    private BigDecimal experienceSimilarity;

    private Integer rankPosition;

}

