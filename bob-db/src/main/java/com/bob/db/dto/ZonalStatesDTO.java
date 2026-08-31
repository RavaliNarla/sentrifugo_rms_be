package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;


@Data
public class ZonalStatesDTO extends BaseDTO {

    @JsonProperty("zonalStateID")
    private UUID id;
    private String stateName;

}
