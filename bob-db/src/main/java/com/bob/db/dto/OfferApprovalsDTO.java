package com.bob.db.dto;

import com.bob.db.enums.OfferApprovalStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class OfferApprovalsDTO extends BaseDTO implements Serializable {

    @JsonProperty("offerApprovalId")
    private UUID id;

    private UUID offerId;
    // --- Nested DTOs instead of IDs ---
    @NotNull(message = "Application details are required")
    private CandidateApplicationsDTO candidateApplication;

    @NotNull(message = "Candidate profile is required")
    private CandidateProfileDTO candidateProfile;

    private JobPositionsDTO designation;

    // --- Primitive Fields ---
    private BigDecimal ctc;
    private BigDecimal bonus;
    private LocalDate joiningDate;
    private LocalDate offerReleaseDate;
    private LocalDate acceptBeforeDate;

    // Kept as UUID assuming you don't need the full Template details yet
    private UUID templateId;
    private String offerFileUrl;

    @NotNull(message = "Approval status is required")
    private OfferApprovalStatus approvalStatus;

    private String latestComments;
}