package com.sentrifugo.rms.recruiterportal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Section 8: shortlist decision is SHORTLIST / REJECT / ON-HOLD. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortlistDecisionRequest {

    public enum Decision {
        SHORTLIST,
        REJECT,
        HOLD
    }

    @NotNull(message = "Decision is required")
    private Decision decision;
}
