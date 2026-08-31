package com.bob.db.dto;

import com.bob.db.enums.RegexPattern;
import com.bob.db.util.excel.ExcelHeader;
import com.bob.db.util.excel.RegexValidate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class DepartmentsDTO extends BaseDTO implements Serializable {

    @JsonProperty("departmentId")
    private UUID id;

    @NotBlank(message = "Department name is required")
    @ExcelHeader(value = "Department Name",isMandatory = true)
    @RegexValidate(regex = RegexPattern.ALPHA_NUMERIC_SPACE_AMP_DASH_UNDERSCORE_AT ,message = "Field should only contain alphabets,numbers, spaces and special characters[&,-,_]")
    private String departmentName;

    @ExcelHeader(value = "Department Description",isMandatory = true)
    @RegexValidate(regex = RegexPattern.DESCRIPTION_ALPHA_NUMERIC_SPACE_PUNCTUATION,message = "Only letters, numbers, spaces, and the special characters . , - _ / ( ) & : ; ' \\\" @ # are allowed.")
    private String departmentDesc;
}
