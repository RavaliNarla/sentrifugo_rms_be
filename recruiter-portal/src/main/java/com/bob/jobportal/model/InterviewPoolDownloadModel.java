package com.bob.jobportal.model;

import com.bob.db.util.excel.ExcelHeader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewPoolDownloadModel {

    @ExcelHeader("Position")
    private String positionName;

    @ExcelHeader("Full Name")
    private String fullName;

    @ExcelHeader("Application Number")
    private String applicationNo;

    @ExcelHeader("Interview Date")
    private String interviewDate;

    @ExcelHeader("Interview Time")
    private String interviewTime;

    @ExcelHeader("Zone")
    private String zone;

    @ExcelHeader("Panel Details")
    private String panelDetails;

    @ExcelHeader("Interview Status")
    private String interviewStatus;

    @ExcelHeader("Score")
    private BigDecimal score;
}
