package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class EducationGroupsDTO extends BaseDTO implements Serializable {

    @JsonProperty("educationGroupId")
    private UUID id;

    private String groupCode;

    private String groupName;

    private Integer displayOrder;
}