package com.bob.candidateportal.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobEligibilityValidationResponse {

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate eligibilityValidationReferenceDate;
    private AgeValidation ageValidation;
    private ExperienceValidation experienceValidation;
    private EducationValidation educationValidation;
    private DocumentValidation documentValidation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgeValidation {
        private Boolean passed;
        private String candidateAge;
        private String allowedAge;
        private List<StateWiseAgeValidation> stateWiseAgeValidations; // For location-wise jobs
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StateWiseAgeValidation {
        private String stateId;
        private String stateName;
        private String cityId;
        private String cityName;
        private Boolean passed;
        private Boolean ageValidationPassed;
        private Boolean stateVacancyValidationPassed;
        private String allowedAge;
    }

//    @Data
//    @Builder
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class ExperienceValidation {
//        private Boolean passed;
//        private Boolean expValidationPassed;
//        private Boolean postQualExpValidationPassed;
//        private String candidateExperience;
//        private String requiredExperience;
//    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExperienceValidation {
        private Boolean passed;
        private String validationMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EducationValidation {
        private Boolean passed;
        private Boolean educationPassed;
        private Boolean certificationPassed;
        private Boolean intermediatePassed;
        private List<String> candidateEducation;
        private List<String> mandatoryEducation;
        private List<String> candidateCertifications;
        private List<String> mandatoryCertifications;
        private List<String> requiredEducation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentValidation {
        private Boolean passed;
        private List<String> requiredDocuments;
        private List<String> submittedDocuments;
    }
}
