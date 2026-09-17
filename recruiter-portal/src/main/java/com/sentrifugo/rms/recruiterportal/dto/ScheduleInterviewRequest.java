package com.sentrifugo.rms.recruiterportal.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** One-day schedule: pick a panel, a day, and a time window; slots are cut by duration. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleInterviewRequest {

    @NotEmpty(message = "Select at least one candidate")
    private List<UUID> candidateIds;

    @NotNull(message = "Select a panel")
    private UUID panelId;

    @NotNull(message = "Interview date is required")
    private LocalDate interviewDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    /** Defaults to 30 minutes when omitted. */
    private Integer durationMinutes;

    /**
     * Interview round. Omit or 1 = first schedule (SHORTLISTED only).
     * For Schedule Next Round, must be current schedule round + 1 (QUALIFIED only).
     */
    private Integer round;
}
