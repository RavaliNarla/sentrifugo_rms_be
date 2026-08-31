package com.bob.db.dto;

import com.bob.db.enums.RegexPattern;
import com.bob.db.util.excel.ExcelHeader;
import com.bob.db.util.excel.RegexValidate;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class CertificationMasterDTO extends BaseDTO{
    @JsonProperty("certificationMasterId")
    private UUID id;

    @ExcelHeader(value = "Certification Name", isMandatory = true)
    @RegexValidate(regex = RegexPattern.ALPHA_NUMERIC_SPACE_AMP_DASH_UNDERSCORE_AT,message = "Field should only contain alphabets, numbers,and special characters[_,&,/,-]")
    private String certificationName;

    @RegexValidate(regex = RegexPattern.DESCRIPTION_ALPHA_NUMERIC_SPACE_PUNCTUATION,message = "Only letters, numbers, spaces, and the special characters . , - _ / ( ) & : ; ' \\\" @ # are allowed.")
    @ExcelHeader(value = "Certification Description",isMandatory = true)
    private String certificationDesc;
}
