package com.bob.db.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class PreOnboardingFamilyDetailsDTO extends BaseDTO implements Serializable {

    private UUID id;

    private UUID preOnboardingId;

    private String fullName;

    private String relationship;

    private LocalDate dateOfBirth;

    private String nationality;

    private Boolean isDependent = false;

    private Boolean isPwd = false;

    private String occupation;

    private BigDecimal annualIncome;

    private String mobileNumber;
}