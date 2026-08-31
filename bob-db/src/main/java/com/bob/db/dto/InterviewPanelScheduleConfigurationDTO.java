package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class InterviewPanelScheduleConfigurationDTO extends BaseDTO {

    @JsonProperty("interviewPanelScheduleConfigurationId")
    private UUID id;

    @NotNull(message = "Application ID is required")
    private UUID applicationId;

    @NotNull(message = "Panel ID is required")
    private UUID panelId;

    @NotNull(message = "Start datetime is required")
    private LocalDateTime startDatetime;

    @NotNull(message = "End datetime is required")
    private LocalDateTime endDatetime;

    private Integer interviewsPerDay;

    private Integer durationMinutes;

}