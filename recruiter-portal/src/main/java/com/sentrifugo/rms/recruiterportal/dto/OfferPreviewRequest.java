package com.sentrifugo.rms.recruiterportal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** Renders the offer letter HTML for review, without saving/emailing anything (SCL_36). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferPreviewRequest {

    /** Optional - when omitted, placeholders like &lt;Candidate_Name&gt; are used. */
    private UUID candidateId;

    @NotNull(message = "Offer template is required")
    private UUID templateId;

    private LocalDate acceptBeforeDate;
    private LocalDate joiningDate;
}
