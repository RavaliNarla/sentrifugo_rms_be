package com.bob.jobportal.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PositionStateDistributionEditDraftRequestModel {
    private UUID stateId;
    private UUID cityId;
    private Integer totalVacancies;
    private String localLanguage;
    @JsonAlias("positionCategoryDistributions")
    private List<PositionCategoryEditDraftRequestModel> categories;
}
