package com.bob.jobportal.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ExamConfigApprovalRequestModel {
    private UUID examConfigId;
    private Boolean approved;
    private String comments;
}
