package com.bob.db.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
public class PreOnboardingAddressDTO extends BaseDTO implements Serializable {

    private UUID id;

    private UUID preOnboardingId;

    private String correspondenceAddressLine1;
    private String correspondenceAddressLine2;
    private String correspondenceAddressLine3;

    private UUID correspondenceCityId;
    private UUID correspondenceStateId;
    private UUID correspondenceDistrictId;

    private String correspondencePincode;

    private String permanentAddressLine1;
    private String permanentAddressLine2;
    private String permanentAddressLine3;

    private UUID permanentCityId;
    private UUID permanentStateId;
    private UUID permanentDistrictId;

    private String permanentPincode;

}