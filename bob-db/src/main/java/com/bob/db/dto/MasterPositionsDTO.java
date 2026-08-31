package com.bob.db.dto;

import com.bob.db.entity.DepartmentsEntity;
import com.bob.db.entity.JobGradeEntity;
import com.bob.db.enums.RegexPattern;
import com.bob.db.util.excel.ExcelDropdown;
import com.bob.db.util.excel.RegexValidate;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterPositionsDTO extends BaseDTO implements Serializable {
    @JsonProperty("masterPositionsId")
    private UUID id;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String positionCode;

    @RegexValidate(regex = RegexPattern.ALPHA_NUMERIC_SPACE_DASH_AMP ,message = "Field should only contain alphabets, numbers, spaces and special characters[&,-]")
    private String positionName;

    private String positionDescription;

    @ExcelDropdown(masterClass = DepartmentsEntity.class, displayField = "departmentName")
    private UUID deptId;

    private Integer totalVacancies;

    private Integer eligibilityAgeMin;

    private Integer eligibilityAgeMax;

    private UUID employmentType;

    private BigDecimal cibilScore;

    @ExcelDropdown(masterClass = JobGradeEntity.class, displayField = "jobGradeCode")
    private UUID gradeId;

    @RegexValidate(regex = RegexPattern.TEXTAREA_BASIC ,message = "Field should only contain alphabets, numbers, spaces and special characters[.,/,-]")
    private String mandatoryEducation;

    @RegexValidate(regex = RegexPattern.TEXTAREA_BASIC ,message = "Field should only contain alphabets, numbers, spaces and special characters[.,/,-]")
    private String preferredEducation;

    @RegexValidate(regex = RegexPattern.TEXTAREA_BASIC ,message = "Field should only contain alphabets, numbers, spaces and special characters[.,/,-]")
    private String mandatoryExperience;

    @RegexValidate(regex = RegexPattern.TEXTAREA_BASIC ,message = "Field should only contain alphabets, numbers, spaces and special characters[.,/,-]")
    private String preferredExperience;

    @RegexValidate(regex = RegexPattern.TEXTAREA_BASIC ,message = "Field should only contain alphabets, numbers, spaces and special characters[.,/,-]")
    private String rolesResponsibilities;
}
