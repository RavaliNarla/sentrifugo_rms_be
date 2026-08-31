package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InterviewScheduleDTO extends BaseDTO implements Serializable {

    @JsonProperty("interviewScheduleId")
    private UUID id;

    private UUID applicationId;

    private UUID candidateId;

    private UUID panelId;

    private LocalDateTime interviewStartAt;

    private LocalDateTime interviewEndAt;

    private Integer interviewDurationMinutes;

    private String meetingLink;

    private UUID zonalOfficeId;

    private BigDecimal finalScore;

    private String interviewStatus;

    private String zonalVerificationStatus;

    private LocalDate zonalSubmitBeforeDate;

    private String zonalHrComments;

    private Boolean lptRequired;

    private String lptStatus;

    private InterviewCentresDTO interviewCentre;
    private InterviewPanelsDTO interviewPanels;
}
