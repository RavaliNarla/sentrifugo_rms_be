package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class JobSelectionProcessDTO extends BaseDTO implements Serializable {

    @JsonProperty("jobSelectionProcessId")
    private UUID id;
    private UUID positionId;

    private String selectionProcedure;
}
