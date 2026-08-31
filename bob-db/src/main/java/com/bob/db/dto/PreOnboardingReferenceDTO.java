package com.bob.db.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class PreOnboardingReferenceDTO extends BaseDTO implements Serializable {

    private UUID id;

    private UUID preOnboardingId;

    private String referenceName;

    private String designation;

    private String organisation;

    private String contactNumber;

    private String emailId;
}