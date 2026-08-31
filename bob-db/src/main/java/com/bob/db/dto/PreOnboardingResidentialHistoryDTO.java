package com.bob.db.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class PreOnboardingResidentialHistoryDTO extends BaseDTO implements Serializable {

    private UUID id;

    private UUID preOnboardingId;

    private LocalDate fromDate;

    private LocalDate toDate;

    private String addressLine1;

    private String addressLine2;

    private String addressLine3;

    private UUID cityId;

    private UUID districtId;

    private UUID stateId;

    private String pincode;

    private UUID preOnboardingDocumentId;
}