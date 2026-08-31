package com.bob.db.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PreOnboardingDisabilityDetailsDTO extends BaseDTO implements Serializable {

    private UUID id;

    private UUID preOnboardingId;

    private UUID disabilityId;

    private String disabilityType;

    private BigDecimal disabilityPercentage;
}