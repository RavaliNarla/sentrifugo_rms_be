package com.bob.jobportal.model;

import com.bob.db.util.excel.ExcelHeader;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class SchedulePoolDownloadModel {
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

    @ExcelHeader("Schedule Status")
    private String scheduleStatus;

}
