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
    private Integer round;
    private String roundName;

    /** The current interviewer's own previously-saved score/rationale/decision, if any (SCL_29). */
    private BigDecimal myScore;
    private String myRationale;
    private String myDecision;
    /** Competency ratings map (1–5) previously saved by this interviewer, if any. */
    private java.util.Map<String, Integer> myCompetencyRatings;
    private String myKeyObservations;

    /** Per-interviewer breakdown for the current round (kept for compatibility). */
    private java.util.List<PanelMemberScoreViewDTO> memberScores;

    /** All rounds with scores, newest first — Interview Pool tooltip. */
    private java.util.List<InterviewRoundFeedbackDTO> roundFeedback;
}
