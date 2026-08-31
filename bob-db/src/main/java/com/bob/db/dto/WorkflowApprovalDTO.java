package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;
import java.io.Serializable;

@Data
@Builder
public class WorkflowApprovalDTO extends BaseDTO implements Serializable {
    @JsonProperty("approvalId")
    private UUID id;
    private String entityType;
    private UUID entityId;
    private Integer stepNumber;
    private String approverRole;
    private UUID approverId;
    private String action;
    private LocalDateTime actionDate;
    private String comments;
    private String status;
}

