package com.bob.jobportal.model;

import com.bob.db.enums.RegexPattern;
import com.bob.db.util.excel.RegexValidate;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateRequisitionEditDraftRequestModel {
    @RegexValidate(regex = RegexPattern.REQ_TITLE_PATTERN,message = "Only letters, numbers, spaces, and the special characters . , - _ / ( ) & : ; ' \" @ # are allowed.")    private String requisitionTitle;
    private LocalDate startDate;

    @RegexValidate(regex = RegexPattern.REQ_DESCRIPTION_PUNCTUATION,message = "Only letters, numbers, spaces, and the special characters . , - _ / ( ) & : ; ' \" @ # % + are allowed.")
    private String requisitionDescription;
    private LocalDate endDate;
    private String requisitionComments;
    private String indentPath;
    private LocalDate cutoffDate;
}
