package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class QualificationGroupMappingDTO extends BaseDTO implements Serializable {

    @JsonProperty("qualificationGroupMappingId")
    private UUID id;

    private UUID educationQualificationId;

    private UUID specializationId;

    private UUID educationGroupId;
}