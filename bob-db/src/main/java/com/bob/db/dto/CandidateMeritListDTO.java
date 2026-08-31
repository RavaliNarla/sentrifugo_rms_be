package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class CandidateMeritListDTO extends BaseDTO{
    @JsonProperty("meritListId")
    private UUID id;

    private UUID candidateId;

    private UUID applicationId;

    private UUID positionId;

    private String applicationNo;

    private String candidateName;

    private UUID categoryId;

    private UUID selectedCategoryId;

    private LocalDate dob;

    private Boolean isPwd;

    private Boolean generalEligible;

    private UUID stateId;

    private UUID cityId;

    private BigDecimal writtenScore;

    private BigDecimal interviewScore;

    private BigDecimal combinedScore;

    private Boolean isQualified;

    private Boolean shortlisted;

    private Boolean selected;

    private Boolean waitlisted;

    private Integer overallRank;

    private Integer categoryRank;

    private Boolean isExServicemen;

    private UUID disabilityTypeId;

    private BigDecimal interviewWeightedScore;

    private BigDecimal examWeightedScore;
}
