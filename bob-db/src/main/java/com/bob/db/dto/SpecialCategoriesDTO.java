package com.bob.db.dto;

import com.bob.db.enums.RegexPattern;
import com.bob.db.util.excel.ExcelHeader;
import com.bob.db.util.excel.RegexValidate;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.UUID;

@Data
public class SpecialCategoriesDTO extends BaseDTO implements Serializable {

    @JsonProperty("specialCategoryId")
    private UUID id;

    @ExcelHeader(value = "Special Category Code",isMandatory = true)
    @RegexValidate(regex = RegexPattern.ALPHA_NUMERIC_SPACE ,message = "Field should only contain alphabets,numbers and spaces")
    private String specialCategoryCode;

    @ExcelHeader(value = "Special Category Name",isMandatory = true)
    @RegexValidate(regex = RegexPattern.ALPHA_NUMERIC_SPACE_AMP_DASH_UNDERSCORE_AT ,message = "Field should only contain alphabets,numbers, spaces and special characters[&,-,_]")
    private String specialCategoryName;

    @ExcelHeader(value = "Special Category Description",isMandatory = true)
    private String specialCategoryDesc;

}
