package com.bob.jobportal.model;

import com.bob.db.enums.RequisitionStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class JobPostingRequestModel {

    @NotEmpty(message = "Job requisition IDs list cannot be empty")
    private List<UUID> jobRequisitionIds;

    @NotNull(message = "Posting status is required")
    private RequisitionStatus postingStatus;

    private String comments;

}
