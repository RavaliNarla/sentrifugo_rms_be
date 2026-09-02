package com.sentrifugo.rms.recruiterportal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** One "Add Panel" row: a panel + a day + a time window, used to auto-generate interview slots. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PanelWindowDTO {

    @NotNull(message = "Panel is required")
    private UUID panelId;
    private String panelName;

    @NotNull(message = "Interview date is required")
    private LocalDate interviewDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Duration is required")
    private Integer durationMinutes;

    /** How many interview slots this window can hold - computed client-side, recomputed server-side. */
    private Integer capacity;
}
