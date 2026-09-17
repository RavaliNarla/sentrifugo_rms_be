package com.sentrifugo.rms.recruiterportal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Section 8 of the requirements doc: shortlist decision is Yes / No / On Hold, not a single button. */
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
