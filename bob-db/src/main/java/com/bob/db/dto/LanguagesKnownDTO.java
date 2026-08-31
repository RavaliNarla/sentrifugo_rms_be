package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;
@Data
public class LanguagesKnownDTO extends BaseDTO implements Serializable {

    @JsonProperty("languageKnownId")
    private UUID id;

    @NotNull(message = "Candidate ID is required")
    private UUID candidateId;

    @NotNull(message = "Language is required")
    private UUID languageId;

    @NotNull(message = "Can read field is required")
    private Boolean canRead = false;

    @NotNull(message = "Can write field is required")
    private Boolean canWrite = false;

    @NotNull(message = "Can speak field is required")
    private Boolean canSpeak = false;

}
