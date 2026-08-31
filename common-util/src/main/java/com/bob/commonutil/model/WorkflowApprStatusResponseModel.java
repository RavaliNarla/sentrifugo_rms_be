package com.bob.commonutil.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WorkflowApprStatusResponseModel {

    private UUID id;
    private String status;
    private LocalDateTime actionDate;
}
