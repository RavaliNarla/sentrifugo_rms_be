package com.bob.masterdata.Model;

import com.bob.db.enums.RegexPattern;
import com.bob.db.util.excel.ExcelHeader;
import com.bob.db.util.excel.RegexValidate;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class JobGradeExcelModel {
    @JsonProperty("jobGradeId")
    private UUID id;

    @ExcelHeader(value = "Job Grade Code",isMandatory = true)
    @RegexValidate(regex = RegexPattern.ALPHA_NUMERIC_SPACE ,message = "Field should only contain alphabets,numbers and spaces")
    private String jobGradeCode;

    @ExcelHeader(value = "Job Grade Description",isMandatory = true)
    private String jobGradeDesc;

    @ExcelHeader(value = "Job Scale",isMandatory = true)
    @RegexValidate(regex = RegexPattern.ALPHA_NUMERIC_SPACE ,message = "Field should only contain alphabets,numbers and spaces")
    private String jobScale;

    @ExcelHeader(value = "Minimum Salary",isMandatory = true)
    @RegexValidate(regex = RegexPattern.NUMBERS_ONLY ,message = "Field should only contain numbers.")
    private String minSalary;

    @ExcelHeader(value = "Maximum Salary",isMandatory = true)
    @RegexValidate(regex = RegexPattern.NUMBERS_ONLY ,message = "Field should only contain numbers")
    private String maxSalary;
}
