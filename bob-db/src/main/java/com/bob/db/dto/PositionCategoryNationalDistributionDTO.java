package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class PositionCategoryNationalDistributionDTO extends BaseDTO implements Serializable {

    @JsonProperty("positionCategoryNationalDistributionId")
    private UUID id;

    private UUID jobPositionId;

    private UUID reservationCategoryId;

    private UUID disabilityCategoryId;

    private Integer vacancyCount;

    private Integer remainingVacancyCount;

    private Integer offersSent;

    private Integer offersAccepted;

    private Boolean isDisability;

}
