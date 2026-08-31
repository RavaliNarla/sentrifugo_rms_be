package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class SpecializationMasterDTO extends BaseDTO implements Serializable {

    @JsonProperty("specializationId")
    private UUID id;

    private String specializationName;

    private UUID educationQualificationsId;

    private String specializationCode;

}
