package com.sentrifugo.rms.recruiterportal.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreSubmitRequest {

    @NotNull(message = "Candidate is required")
    private UUID candidateId;

    @NotNull(message = "Score is required")
    @DecimalMin(value = "1", message = "Score must be between 1 and 10")
    @DecimalMax(value = "10", message = "Score must be between 1 and 10")
    private BigDecimal score;

    /** Renamed from "Comment" - interviewer must justify the score given. */
    private String rationale;

    /** SELECT / REJECT / HOLD. */
    private String decision;
}
