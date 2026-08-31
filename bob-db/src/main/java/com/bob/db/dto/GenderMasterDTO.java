package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class GenderMasterDTO extends BaseDTO implements Serializable {

    @JsonProperty("genderId")
    private UUID id;

    private String gender;
}
