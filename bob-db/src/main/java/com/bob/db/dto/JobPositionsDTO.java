package com.bob.db.dto;

import com.bob.db.enums.PositionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.io.Serializable;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobPositionsDTO extends BaseDTO implements Serializable {
    @JsonProperty("positionId")
    private UUID id;

    private UUID parentPositionId;

    @NotNull(message = "Requisition ID is required")
    private UUID requisitionId;

    @NotNull(message = "Master position ID is required")
    private UUID masterPositionId;

    @NotNull(message = "Department ID is required")
    private UUID deptId;

    @NotNull(message = "Total vacancies is required")
    private Integer totalVacancies;

    private Integer remainingTotalVacancies;

    private Boolean isHiringCompleted;

    private Integer eligibilityAgeMin;

    private Integer eligibilityAgeMax;

    @NotNull(message = "Employment type is required")
    private UUID employmentType;

    private BigDecimal cibilScore;

    @NotNull(message = "Grade ID is required")
    private UUID gradeId;

    private String mandatoryEducation;

    private String preferredEducation;

    private String mandatoryExperience;

    private String preferredExperience;
    
    private String rolesResponsibilities;

    private PositionStatus positionStatus;

    private Integer contractYears;

    private Boolean isLocationPreferenceEnabled;

    private Boolean isLocationWise;

    private BigDecimal totalExperience;

    private Integer mandatoryExperienceMonths;

    private Integer preferredExperienceMonths;

    private String indentPath;

    private UUID approvedBy;

    private LocalDate approvedOn;

    private Boolean isMedicalRequired;

    private JsonNode mandatoryEduRulesJson;

    private JsonNode preferredEduRulesJson;

    private String indentName;

    private String indentOthers;

    private LocalDate cutoffDate;

    private Boolean isMandatoryExpMonthsEduWise;

    private Boolean isPreferredExpMonthsEduWise;

    private JsonNode mandatoryExpMonthsEduWise;

    private JsonNode preferredExpMonthsEduWise;

    private Boolean isProficientInLocalLanguage;

    // Age relaxation applicable to widowed, divorced and judicially separated women.
    private Boolean isAgeRelWdsWomen;

    // Age relaxation applicable to 1984 riots victim family.
    private Boolean isAgeRelRiotVictimFamily;

    private Boolean isIntermediateRequired;

    private JsonNode dynamicFields;

//    private List<PositionRequiredDocumentsDTO> positionRequiredDocuments;

    private List<PositionStateDistributionDTO> positionStateDistributions;

    private List<PositionCategoryNationalDistributionDTO> positionCategoryNationalDistributions;

    private List<JobPositionExclusionsDTO> jobPositionExclusion;

}
