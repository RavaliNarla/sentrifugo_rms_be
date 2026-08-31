package com.bob.db.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class PreOnboardingOtherDetailsDTO extends BaseDTO implements Serializable {

    private UUID id;

    private UUID preOnboardingId;

    private String identificationMark;

    private String practicalTrainingDetails;

    private String publicationDetails;

    private Boolean pursuingFurtherCourse = false;

    private String furtherCourseDetails;

    private Boolean memberOfProfessionalBody = false;

    private String professionalBodyDetails;

    private Boolean memberOfPoliticalBody = false;

    private String politicalBodyDetails;

    private Boolean criminalCaseDeclared = false;

    private String criminalCaseDetails;

    private String additionalInformation;
}