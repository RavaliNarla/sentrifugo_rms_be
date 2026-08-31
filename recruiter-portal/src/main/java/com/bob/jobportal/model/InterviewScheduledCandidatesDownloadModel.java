package com.bob.jobportal.model;

import com.bob.db.util.excel.ExcelHeader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewScheduledCandidatesDownloadModel {

    @ExcelHeader("Requisition")
    private String requisitionName;

    @ExcelHeader("Position")
    private String positionName;

    @ExcelHeader("Department")
    private String departmentName;

    @ExcelHeader("Application No")
    private String applicationNo;

    @ExcelHeader("Candidate Name")
    private String candidateName;

    @ExcelHeader("Interview Centre")
    private String interviewCentre;

    @ExcelHeader("Panel Name")
    private String panelName;

    @ExcelHeader("Interview Date")
    private String interviewDate;

    @ExcelHeader("Interview Start Time")
    private String interviewStartTime;

    @ExcelHeader("Interview End Time")
    private String interviewEndTime;
}
