package com.bob.jobportal.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class RequisitionReinitializeRequestModel {

    @NotNull(message = "Parent requisition id is required")
    private UUID parentRequisitionId;

    @NotEmpty(message = "At least one position must be selected")
    private List<UUID> positionIds;

    @NotBlank(message = "Requisition title is required")
    private String requisitionTitle;

    private String requisitionDescription;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private LocalDate cutoffDate;
}
