package com.bob.db.dto;

import com.bob.db.enums.PreOnboardingStatus;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class PreOnboardingDTO extends BaseDTO implements Serializable {

    private UUID id;

    private UUID candidateId;

    private UUID applicationId;

    private PreOnboardingStatus onboardingStatus;

    private String joiningLocation;

    private LocalDate expectedJoiningDate;

    private LocalDate actualJoiningDate;

    private String remarks;
}