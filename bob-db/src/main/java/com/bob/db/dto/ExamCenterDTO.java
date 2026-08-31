package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.Data;

import java.util.UUID;

@Data
public class ExamCenterDTO {
    @JsonProperty("examCentreId")
    private UUID id;
    private String examCentre;
}
