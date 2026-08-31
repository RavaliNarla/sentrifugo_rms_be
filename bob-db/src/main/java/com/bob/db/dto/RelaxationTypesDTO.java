package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import lombok.Data;
import java.io.Serializable;
import java.util.UUID;

@Data
public class RelaxationTypesDTO extends BaseDTO implements Serializable {
    @JsonProperty("relaxationTypeId")
    private UUID id;

    private String relaxationTypeName;

    private String description;

    private JsonNode others;

    private String input;

    private String operator;
}
