package com.bob.db.dto;

import com.bob.db.enums.CompensationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class CandidateCompensationDTO extends BaseDTO implements Serializable {

    @JsonProperty("candidateCompensationId")
    private UUID id;
    private UUID candidateId;
    private CandidateProfileDTO candidateProfile;
    private CandidateApplicationsDTO application;
    private UUID interviewScheduleId;
//    private UUID panelId;

    private LocalDate submitBeforeDate;

    private BigDecimal currentCtc;
    private BigDecimal expectedCtc;
    private BigDecimal hike;
    private BigDecimal agreedCtc;

    private BigDecimal fixedPay;
    private BigDecimal variablePay;
    private BigDecimal joiningBonus;

    private String recruiterComments;
    private String panelComments;

    private CompensationStatus compensationStatus;
}