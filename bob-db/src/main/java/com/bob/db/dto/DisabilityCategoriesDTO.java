package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class DisabilityCategoriesDTO extends BaseDTO implements Serializable {

    @JsonProperty("disabilityCategoryId")
    private UUID id;

    private String disabilityCode;

    private String disabilityName;

    private Integer displayOrder;
}
