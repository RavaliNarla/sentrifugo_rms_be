package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CandidateEmbeddingsDTO extends BaseDTO implements Serializable {

    @JsonProperty("candidateEmbeddingId")
    private UUID id;

    private UUID candidateId;

    private UUID applicationId;

    private List<Double> experienceEmbedding;

    private List<Double> educationEmbedding;

    private BigDecimal experienceScore;

    private BigDecimal educationScore;

    private String experienceEmbeddingRawText;

    private String educationEmbeddingRawText;

}

