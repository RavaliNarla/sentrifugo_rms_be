package com.bob.jobportal.model;

import lombok.Data;

import java.util.UUID;

@Data
public class PositionCategoryEditDraftRequestModel {
    private UUID reservationCategoryId;
    private UUID disabilityCategoryId;
    private Integer vacancyCount;
    private Boolean isDisability;
}
