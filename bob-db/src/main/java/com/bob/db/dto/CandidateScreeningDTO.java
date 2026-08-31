package com.bob.db.dto;

import com.bob.db.enums.CandidateScreeningCriteriaStatus;
import com.bob.db.enums.CandidateScreeningShortlistedStatus;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CandidateScreeningDTO extends BaseDTO {

    @JsonProperty("screeningId")
    private UUID id;

    @NotNull(message = "Application ID is required")
    private UUID applicationId;

    @NotNull(message = "Candidate ID is required")
    private UUID candidateId;


    @NotNull(message = "Work criteria met status is required")
    private CandidateScreeningCriteriaStatus isWorkCriteriaMet = CandidateScreeningCriteriaStatus.DEFAULT;

    @NotNull(message = "Age criteria met status is required")
    private CandidateScreeningCriteriaStatus isAgeCriteriaMet = CandidateScreeningCriteriaStatus.DEFAULT;

    @NotNull(message = "Education criteria met status is required")
    private CandidateScreeningCriteriaStatus isEducationCriteriaMet  = CandidateScreeningCriteriaStatus.DEFAULT;

    @NotNull(message = "Shortlisted status is required")
    private CandidateScreeningShortlistedStatus isShortlisted = CandidateScreeningShortlistedStatus.DEFAULT;

    private Boolean isEligible = false;

    private String workCriteriaRemark;

    private String ageCriteriaRemark;

    private String educationCriteriaRemark;

    private String finalScreeningRemark;

    @FutureOrPresent(message = "Submit before date must be in the present or future")
    private LocalDate submitBeforeDate;

    @JsonAlias({"additionallyRequiredDocuments", "additionalDocuments"})
    private List<String> additionalDocumentNames;

    private Boolean isScreeningCompleted;

}

