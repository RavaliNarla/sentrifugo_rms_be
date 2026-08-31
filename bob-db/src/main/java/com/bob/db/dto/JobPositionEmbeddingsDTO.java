package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobPositionEmbeddingsDTO extends BaseDTO implements Serializable {

    @JsonProperty("jobPositionEmbeddingId")
    private UUID id;

    private UUID positionId;

    private List<Double> experienceEmbedding;

    private List<Double> educationEmbedding;

}

