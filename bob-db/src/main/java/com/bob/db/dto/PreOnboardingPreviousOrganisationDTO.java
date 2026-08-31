package com.bob.db.dto;

import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class PreOnboardingPreviousOrganisationDTO extends BaseDTO implements Serializable {

    private UUID id;

    private UUID preOnboardingId;

    private String pranCardNumber;

    private String employeeCode;

    private String organisationName;

    private String organisationAddress;

    private String designation;

    private String workDescription;

    private BigDecimal lastDrawnSalary;

    private LocalDate fromDate;

    private LocalDate toDate;

    private String reasonForLeaving;

    private String employerEmail;

    private String employerContactNo;

    private Boolean isCurrentlyWorking = false;
}