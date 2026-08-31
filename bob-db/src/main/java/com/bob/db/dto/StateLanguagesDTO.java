package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class StateLanguagesDTO extends BaseDTO implements Serializable {
    @JsonProperty("stateLanguageId")
    private UUID id;

    private UUID stateId;

    private UUID languageId;

    private Boolean isPrimary = false;

}
