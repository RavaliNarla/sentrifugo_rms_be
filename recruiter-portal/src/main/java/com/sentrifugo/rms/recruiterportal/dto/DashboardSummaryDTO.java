package com.sentrifugo.rms.recruiterportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {
    private long totalRequisitions;
    private long pendingApprovalRequisitions;
    private long approvedRequisitions;
    private long fulfilledRequisitions;
    private long totalCandidates;
    private long shortlistedCandidates;
    private long scheduledCandidates;
    private long qualifiedCandidates;
    private long offersSent;
}
