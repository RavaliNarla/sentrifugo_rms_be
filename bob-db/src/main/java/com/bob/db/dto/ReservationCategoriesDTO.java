package com.bob.db.dto;

import com.bob.db.enums.RegexPattern;
import com.bob.db.enums.ReservationType;
import com.bob.db.util.excel.ExcelHeader;
import com.bob.db.util.excel.RegexValidate;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
import java.util.UUID;

@Data
public class ReservationCategoriesDTO extends BaseDTO implements Serializable {

    @JsonProperty("reservationCategoriesId")
    private UUID id;

    @ExcelHeader(value = "Category Code",isMandatory = true)
    @RegexValidate(regex = RegexPattern.ALPHA_NUMERIC_SPACE ,message = "Field should only contain alphabets,numbers and spaces")
    private String categoryCode;

    @ExcelHeader(value = "Category Name",isMandatory = true)
    @RegexValidate(regex = RegexPattern.ALPHA_NUMERIC_SPACE_AMP_DASH_UNDERSCORE_AT ,message = "Field should only contain alphabets,numbers, spaces and special characters[&,-,_]")
    private String categoryName;

    @ExcelHeader(value = "Category Description",isMandatory = true)
    @RegexValidate(regex = RegexPattern.DESCRIPTION_ALPHA_NUMERIC_SPACE_PUNCTUATION,message = "Only letters, numbers, spaces, and the special characters . , - _ / ( ) & : ; ' \\\" @ # are allowed.")
    private String categoryDesc;

    private Integer displayOrder;

    private ReservationType reservationType = ReservationType.VERTICAL;
}
