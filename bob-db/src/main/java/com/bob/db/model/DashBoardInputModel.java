package com.bob.db.model;

import com.bob.db.enums.DashboardScreen;
import com.bob.db.enums.DashboardDateRangePreset;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class DashBoardInputModel {

    @Builder.Default
    private DashboardDateRangePreset dateRangePreset = null;
    private Integer fyYear;
    private Integer cyYear;
    private Integer quarter;  // 1-4
    private LocalDate fromDate;
    private LocalDate toDate;
    private UUID departmentId;
    private UUID positionId;
    private String zone;
    private UUID stateId;
    private UUID cityId;
    private UUID recruiterId;
    private UUID employmentTypeId;
    @Builder.Default
    private Boolean isReinitialized = true;

    //only for report downloads
    private String extension;
    private DashboardScreen reportScreen;
    private String committee;
}
