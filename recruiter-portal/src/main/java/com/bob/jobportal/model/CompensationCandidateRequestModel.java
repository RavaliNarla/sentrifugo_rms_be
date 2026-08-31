package com.bob.jobportal.model;

import com.bob.db.enums.CompensationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;
@Data
public class CompensationCandidateRequestModel {
    private String searchText;
    @NotNull(message = "Position ID cannot be null")
    private UUID positionId;
    private List<CompensationStatus> statusList;
    private int page = 0; // Default to first page
    private int size = 5; // Default page size
}
