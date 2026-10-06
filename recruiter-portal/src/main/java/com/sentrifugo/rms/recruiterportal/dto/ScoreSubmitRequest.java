package com.sentrifugo.rms.recruiterportal.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreSubmitRequest {

    @NotNull(message = "Candidate is required")
    private UUID candidateId;

    /** 1–10 when provided. All-or-none with rationale + decision validated in service. */
    @DecimalMin(value = "1", message = "Score must be between 1 and 10")
    @DecimalMax(value = "10", message = "Score must be between 1 and 10")
    private BigDecimal score;

    private String rationale;

    /** STRONG_HIRE / HIRE / HOLD / DO_NOT_HIRE (legacy SELECT/REJECT/HOLD still accepted). */
    private String decision;

    /**
     * Competency ratings (1–5). Keys:
     * TECHNICAL_KNOWLEDGE, RELEVANT_EXPERIENCE, COMMUNICATION, PROBLEM_SOLVING, ATTITUDE_APPROACH.
     * All five are required when submitting a score.
     */
    private Map<String, Integer> competencyRatings;

    private String keyObservations;
}
