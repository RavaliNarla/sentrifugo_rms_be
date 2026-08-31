package com.bob.jobportal.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SummaryModel {
    private UUID stateId;
    private List<CategoryWiseCountModel> categorySummaries;
    private Integer totalAppearedCount;
    private Integer totalVacancyCount;
    private Integer totalQualifiedWithoutRelaxation;
}
