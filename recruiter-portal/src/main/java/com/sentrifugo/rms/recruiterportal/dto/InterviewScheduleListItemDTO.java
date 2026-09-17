package com.sentrifugo.rms.recruiterportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Row for the Committee Management → Interview Schedules read-only tab. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewScheduleListItemDTO {
    private UUID id;
    private LocalDate interviewDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private String candidateName;
    private String candidateEmail;
    private String candidateStatus;
    private String panelName;
    private List<String> panelMemberNames;
    private String positionTitleName;
    private String locationName;
    private Integer round;
}
