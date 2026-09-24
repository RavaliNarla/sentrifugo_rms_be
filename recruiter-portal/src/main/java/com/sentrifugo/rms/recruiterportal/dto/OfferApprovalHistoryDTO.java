package com.sentrifugo.rms.recruiterportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferApprovalHistoryDTO {
    private UUID id;
    private String approverName;
    private LocalDateTime approvalDate;
    private String status;
    private String comments;
}
