package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.UUID;

@Data
public class TemplatesDTO extends BaseDTO implements Serializable {

    @JsonProperty("templateId")
    private UUID id;

    private String templateType;

    private String templateName;

    private String templateDesc;

    private String filePath;

    private JsonNode others;

}
