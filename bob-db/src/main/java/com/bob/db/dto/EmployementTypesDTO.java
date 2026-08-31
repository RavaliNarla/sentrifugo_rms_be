package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmployementTypesDTO extends BaseDTO {

    @JsonProperty("employementTypeId")
    private UUID id;

    private String typeCode;

    private String typeName;
}
