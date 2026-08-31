package com.bob.jobportal.model;

import com.bob.commonutil.util.AppConstants;
import com.bob.db.entity.InterviewCentresEntity;
import com.bob.db.entity.StateEntity;
import com.bob.db.util.excel.ExcelDropdown;
import com.bob.db.util.excel.ExcelHeader;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CandidateOfferExcelModel {

    @ExcelHeader(AppConstants.OFFER_ID_HEADER)
    private UUID offerId;

    @ExcelHeader(value = "Registration No",isMandatory = true)
    private String regNo;

    @ExcelHeader(value = "Candidate Name",isMandatory = true)
    private String candidateFullName;

    @ExcelHeader(value = "Designation",isMandatory = true)
    private String designation;

    @ExcelHeader(value = "Caste",isMandatory = true)
    private String reservationCategory;

    @ExcelHeader(value = "Score",isMandatory = true)
    private BigDecimal finalScore;

    @ExcelHeader(value = "Status",isMandatory = true)
    private String status;

    @ExcelHeader("Select List")
    private String selectList;

    @ExcelHeader("Wait List")
    private String waitList;

    @ExcelHeader(value = "Location",isMandatory = true)
    @ExcelDropdown(masterClass = InterviewCentresEntity.class, displayField = "displayName")
    private UUID location;
}
