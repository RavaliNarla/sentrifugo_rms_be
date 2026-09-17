package com.sentrifugo.rms.recruiterportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** L1/L2 offer-letter approval action (Section 12/16 of the requirements doc). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferApprovalActionRequest {
    /** Candidate ids whose current offer should be approved/rejected. */
    private List<UUID> candidateIds;
    private boolean approve;
    private String comments;
}
