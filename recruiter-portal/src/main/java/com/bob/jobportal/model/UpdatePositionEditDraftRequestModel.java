package com.bob.jobportal.model;

import com.bob.db.entity.JobPositionExclusionsEditRequestEntity;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class UpdatePositionEditDraftRequestModel {
    private UUID masterPositionId;
    private UUID deptId;
    private Boolean isLocationWise;

    private Integer totalVacancies;
    private Integer eligibilityAgeMin;
    private Integer eligibilityAgeMax;
    private UUID employmentType;
    private BigDecimal cibilScore;
    private UUID gradeId;
    private String mandatoryEducation;
    private String preferredEducation;
    private String mandatoryExperience;
    private String preferredExperience;
    private String rolesResponsibilities;
    private Integer contractYears;
    private Boolean isLocationPreferenceEnabled;
    private Boolean isMandatoryExpMonthsEduWise;
    private Boolean isPreferredExpMonthsEduWise;
    private BigDecimal totalExperience;
    private Integer mandatoryExperienceMonths;
    private Integer preferredExperienceMonths;
    private String indentPath;
    private UUID approvedBy;
    private LocalDate approvedOn;
    private Boolean isMedicalRequired;
    private JsonNode mandatoryEduRulesJson;
    private JsonNode preferredEduRulesJson;
    private JsonNode mandatoryExpMonthsEduWise;
    private JsonNode preferredExpMonthsEduWise;
    private String indentName;
    private String indentOthers;
    private Boolean isProficientInLocalLanguage;
    private Boolean isAgeRelWdsWomen;
    private Boolean isAgeRelRiotVictimFamily;
    private Boolean isIntermediateRequired;
    private JsonNode dynamicFields;

    @JsonAlias("jobPositionExclusion")
    private List<JobPositionExclusionsEditRequestModel> exclusions;

    @JsonAlias("positionStateDistributions")
    private List<PositionStateDistributionEditDraftRequestModel> stateDistributions;

    @JsonAlias("positionCategoryNationalDistributions")
    private List<PositionCategoryEditDraftRequestModel> nationalDistributions;
}
