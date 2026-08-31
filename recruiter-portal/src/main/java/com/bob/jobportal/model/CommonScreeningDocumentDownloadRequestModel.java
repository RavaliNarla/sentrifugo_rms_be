package com.bob.jobportal.model;

import com.bob.db.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CommonScreeningDocumentDownloadRequestModel {

    @NotBlank(message = "Document type cannot be blank")
    private String documentType;

    @NotNull(message = "Position ID cannot be null")
    private List<UUID> positionIds;

    @NotBlank(message = "Screen name cannot be blank")
    private String screenName;

    private boolean rank;

    private List<CandidateApplicationStatus> candidateApplicationStatuses;

    private List<InterviewSchedulingStatus> interviewSchedulingStatuses;

    private List<InterviewSchedulingApprovalStatus> interviewSchedulingApprovalStatuses;

    private List<CompensationStatus> compensationStatuses;

    private UUID categoryId;

    private UUID stateId;

}
