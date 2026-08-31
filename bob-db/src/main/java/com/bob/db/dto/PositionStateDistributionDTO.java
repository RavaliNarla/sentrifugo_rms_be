package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
public class PositionStateDistributionDTO extends BaseDTO implements Serializable {

    @JsonProperty("positionStateDistributionId")
    private UUID id;

    private UUID positionId;

    private UUID stateId;

    private UUID cityId;

    private Integer totalVacancies;

    private Integer remainingTotalVacancies;

    private Integer offersSent;

    private Integer offersAccepted;

    private String localLanguage;

    private List<PositionCategoryDistributionDTO> positionCategoryDistributions;

}
