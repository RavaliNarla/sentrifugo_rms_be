package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.UUID;

@Data
public class RequestTypesDTO extends BaseDTO implements Serializable {

    @JsonProperty("requestTypeId")
    private UUID id;

    private String requestName;

}
