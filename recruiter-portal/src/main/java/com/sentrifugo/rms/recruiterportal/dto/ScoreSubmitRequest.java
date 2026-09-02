package com.sentrifugo.rms.recruiterportal.dto;

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
    private BigDecimal score;

    private String comments;
}
