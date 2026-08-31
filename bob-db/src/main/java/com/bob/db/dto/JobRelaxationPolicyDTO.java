package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.UUID;
import java.io.Serializable;

@Data
public class JobRelaxationPolicyDTO extends BaseDTO implements Serializable {

    @JsonProperty("jobRelaxationPolicyId")
    private UUID Id;

    private JsonNode relaxation;

    private String relaxationPolicyNumber;
}
