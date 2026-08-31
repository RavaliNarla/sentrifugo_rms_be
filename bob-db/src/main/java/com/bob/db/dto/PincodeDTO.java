package com.bob.db.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class PincodeDTO extends BaseDTO implements Serializable {

    @JsonProperty("pincodeId")
    private UUID id;

    private String pin;

    private UUID cityId;

}
