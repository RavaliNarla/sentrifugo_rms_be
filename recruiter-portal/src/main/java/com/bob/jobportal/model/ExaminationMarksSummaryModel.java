package com.bob.jobportal.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ExaminationMarksSummaryModel {
    private UUID positionId;
    private Boolean isStateWise;
    private Boolean isFinalized;
    private List<SummaryModel> overallMarksSummary;
}

