package com.bob.db.dto;

import com.bob.db.enums.RegexPattern;
import com.bob.db.util.excel.RegexValidate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class CandidateAddressDTO extends BaseDTO implements Serializable {

    @JsonProperty("addressId")
    private UUID id;
    
    @NotNull(message = "Candidate ID is required")
    private UUID candidateId;

    @NotBlank(message = "Address line 1 is required")
    @RegexValidate(regex = RegexPattern.ADDRESS_LINE_PUNCTUATION,message = "Only letters, numbers, spaces, and the following special characters are allowed: . , - _ / ( ) & : ; ' \" @ #.")
    private String addressLine1;

    @RegexValidate(regex = RegexPattern.ADDRESS_LINE_PUNCTUATION,message = "Only letters, numbers, spaces, and the following special characters are allowed: . , - _ / ( ) & : ; ' \" @ #.")
    private String addressLine2;

    private String landmark;

    @NotNull(message = "State is required")
    private UUID stateId;

    private UUID districtId;

    @NotNull(message = "City is required")
    private String city;

    private String pincode;

    @RegexValidate(regex = RegexPattern.ADDRESS_LINE_PUNCTUATION,message = "Only letters, numbers, spaces, and the following special characters are allowed: . , - _ / ( ) & : ; ' \" @ #.")
    private String permanentAddressLine1;

    @RegexValidate(regex = RegexPattern.ADDRESS_LINE_PUNCTUATION,message = "Only letters, numbers, spaces, and the following special characters are allowed: . , - _ / ( ) & : ; ' \" @ #.")
    private String permanentAddressLine2;

    private String permanentLandmark;

    private UUID permanentStateId; // Need to check

    private UUID permanentDistrictId;

    private String permanentCity;

    private String permanentPincode;
}
