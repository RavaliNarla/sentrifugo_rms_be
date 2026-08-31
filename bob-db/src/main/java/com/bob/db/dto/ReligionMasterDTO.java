package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class ReligionMasterDTO extends BaseDTO implements Serializable {
    @JsonProperty("religionId")
    private UUID id;

    private String religion;
}
