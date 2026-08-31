package com.bob.db.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class EducationTypeMasterDTO extends BaseDTO implements Serializable {

    @JsonProperty("educationTypeId")
    private UUID id;

    private String educationType;

}
