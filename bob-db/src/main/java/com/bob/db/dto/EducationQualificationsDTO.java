package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.UUID;

@Data

public class EducationQualificationsDTO extends BaseDTO implements Serializable {

    @JsonProperty("educationQualificationsId")
    private UUID id;

    private UUID levelId;

    private String qualificationCode;

    private String qualificationName;

    private Integer displayOrder;

}

