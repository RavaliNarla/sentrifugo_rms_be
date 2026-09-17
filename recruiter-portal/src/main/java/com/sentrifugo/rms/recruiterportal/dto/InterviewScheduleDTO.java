package com.sentrifugo.rms.recruiterportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewScheduleDTO {
    private UUID id;
    private UUID candidateId;
    private String candidateName;
    private String applicationStatus;
    private UUID panelId;
    private String panelName;
    private LocalDate interviewDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private BigDecimal finalScore;
    private Integer membersScored;
    private Integer membersTotal;

    /** The current interviewer's own previously-saved score/rationale/decision, if any (SCL_29). */
    private Integer myScore;
    private String myRationale;
    private String myDecision;
}
