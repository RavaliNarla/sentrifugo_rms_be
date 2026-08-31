package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class MaritalStatusMasterDTO extends BaseDTO {
    private String maritalStatus;

    @JsonProperty("maritalStatusId")
    private UUID id;
}
