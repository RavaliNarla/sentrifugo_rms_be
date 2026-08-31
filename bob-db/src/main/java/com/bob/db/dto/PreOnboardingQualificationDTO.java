package com.bob.db.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class PreOnboardingQualificationDTO extends BaseDTO implements Serializable {

    private UUID id;

    private UUID preOnboardingId;

    private UUID qualificationName;

    private String institutionName;

    private UUID specialization;

    private String universityName;

    private BigDecimal percentage;

    private LocalDate passingDate;

    private String division;

    private UUID courseNature;

    private Boolean isCurrentlyWorking = false;

    private UUID educationId;
}