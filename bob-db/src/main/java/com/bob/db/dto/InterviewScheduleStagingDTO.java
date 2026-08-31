package com.bob.db.dto;

import com.bob.db.enums.InterviewSchedulingApprovalStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ser.Serializers;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InterviewScheduleStagingDTO extends BaseDTO {
    @JsonProperty("interviewScheduleStagingId")
    private UUID id;

    // For Input/Output: Full application details
    @NotNull(message = "Application data is required")
    private CandidateApplicationsDTO application;

    @NotNull(message = "Candidate ID is required")
    private UUID candidateId;

    // For Display: Transient candidate details
    private CandidateProfileDTO candidateProfile;

    @NotNull(message = "Panel ID is required")
    private UUID panelId;

    @NotNull(message = "Zonal Office ID is required")
    private UUID zonalOfficeId;

    @NotNull(message = "Interview Start Time is required")
    private LocalDateTime interviewStartAt;

    @NotNull(message = "Interview End Time is required")
    private LocalDateTime interviewEndAt;

    private Integer interviewDurationMinutes;

    private InterviewSchedulingApprovalStatus interviewSchedulingApprovalStatus;

    private String remarks;

    private boolean rescheduled=false;

    private InterviewCentresDTO interviewCentre;
}
