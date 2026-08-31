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
public class OfferApprovalHistoryDTO extends BaseDTO implements Serializable {

    @JsonProperty("offerApprovalHistoryId")
    private UUID id;

    private UUID offerId;

    @NotNull(message = "Offer Approval ID is required")
    private UUID offerApprovalId;

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
    private UUID templateId;
    private String offerFileUrl;

    @NotNull(message = "Action By is required")
    private UUID actionBy;

    @NotNull(message = "Action Taken is required")
    private String actionTaken;

    private String comments;

    @NotNull(message = "Approval status is required")
    private OfferApprovalStatus approvalStatus;

    private String fullName;

    private Integer combinedScore;

    private UUID stateId;
    private UUID cityId;
}