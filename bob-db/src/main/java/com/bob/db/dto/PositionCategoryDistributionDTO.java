package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class PositionCategoryDistributionDTO extends BaseDTO implements Serializable {

    @JsonProperty("positionCategoryDistributionId")
    private UUID id;

    private UUID stateDistributionId;

    private UUID reservationCategoryId;

    private UUID disabilityCategoryId;

    private Integer vacancyCount;

    private Integer remainingVacancyCount;

    private Integer offersSent;

    private Integer offersAccepted;

    private Boolean isDisability;

}
