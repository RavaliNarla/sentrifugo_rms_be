package com.bob.db.dto;

import com.bob.db.enums.ActionEnum;
import com.bob.db.enums.PositionPanelStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class PositionPanelDTO extends BaseDTO {

    @JsonProperty("positionPanelId")
    private UUID id;

    @NotNull(message = "Position ID is required")
    private UUID positionId;

    @NotNull(message = "Interview panels is required")
    @Valid
    private InterviewPanelsDTO interviewPanel;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Positive(message = "Sequence number must be positive")
    private Integer sequenceNo;

    private PositionPanelStatus positionPanelStatus;

    private boolean canEdit=true;
    
    private ActionEnum actionEnum;

    private String positionName;

}
