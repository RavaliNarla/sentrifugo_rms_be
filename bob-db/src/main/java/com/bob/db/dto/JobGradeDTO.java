package com.bob.db.dto;

import com.bob.db.enums.RegexPattern;
import com.bob.db.util.excel.RegexValidate;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.io.Serializable;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobGradeDTO extends BaseDTO implements Serializable {
    @JsonProperty("jobGradeId")
    private UUID id;

    @RegexValidate(regex = RegexPattern.ALPHA_NUMERIC_SPACE ,message = "Field should only contain alphabets,numbers and spaces")
    private String jobGradeCode;

    @RegexValidate(regex = RegexPattern.DESCRIPTION_ALPHA_NUMERIC_SPACE_PUNCTUATION,message = "Only letters, numbers, spaces, and the special characters . , - _ / ( ) & : ; ' \\\" @ # are allowed.")
    private String jobGradeDesc;

    @RegexValidate(regex = RegexPattern.ALPHA_NUMERIC_SPACE ,message = "Field should only contain alphabets,numbers and spaces")
    private String jobScale;


    private BigDecimal minSalary;

    private BigDecimal maxSalary;

    private LocalDate effectiveStateDate;

    private LocalDate effectiveEndDate;

}
