package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class LanguageMasterDTO {
    @JsonProperty("languageId")
    private String id;
    private String languageName;
    private UUID stateId;
}
