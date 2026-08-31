package com.bob.db.dto;

import com.bob.db.enums.RegexPattern;
import com.bob.db.util.excel.RegexValidate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.Data;
import java.io.Serializable;
import java.util.UUID;

@Data
public class DocumentTypesDTO extends BaseDTO implements Serializable {
    @JsonProperty("documentTypeId")
    private UUID id;

    @RegexValidate(regex = RegexPattern.ALPHA_NUMERIC_SPACE ,message = "Field should only contain alphabets,numbers and spaces")
    private String documentName;

    @RegexValidate(regex = RegexPattern.DESCRIPTION_ALPHA_NUMERIC_SPACE_PUNCTUATION,message = "Only letters, numbers, spaces, and the special characters . , - _ / ( ) & : ; ' \\\" @ # are allowed.")
    private String documentDesc;

    private String docCode;

    private String docType;

    private Boolean isEditable=true;

    private Boolean isActive =true;

    private Boolean isRequired=false;

}
