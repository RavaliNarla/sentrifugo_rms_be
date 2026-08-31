package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class MedicalCentresDTO extends BaseDTO implements Serializable {
    @JsonProperty("medicalCentreId")
    private UUID id;
    private String medicalCentre;
}
