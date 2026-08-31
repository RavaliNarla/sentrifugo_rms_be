package com.bob.db.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PreOnboardingGratuityNominationDTO extends BaseDTO implements Serializable {

    private UUID id;

    private UUID preOnboardingId;

    private String nomineeName;

    private String nomineeAddress;

    private String nomineeRelationship;

    private Integer nomineeAge;

    private BigDecimal sharePercentage;
}