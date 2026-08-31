package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PanelMembersScoreDTO extends BaseDTO implements Serializable {
    @JsonProperty("panelMembersScoreId")
    private UUID id;

    private UUID applicationId;

    private UUID scheduledInterviewId;

    private UUID candidateId;

    private UUID panelId;

    private UUID panelMemberId;

    private BigDecimal panelScore;

    private String panelComments;

    private UUID interviewCenterId;

    private Boolean isAbsent;
}
