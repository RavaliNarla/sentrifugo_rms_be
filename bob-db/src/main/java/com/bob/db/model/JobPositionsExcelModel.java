package com.bob.db.model;

import com.bob.db.entity.*;
import com.bob.db.enums.RegexPattern;
import com.bob.db.util.DBConstants;
import com.bob.db.util.excel.ExcelDropdown;
import com.bob.db.util.excel.ExcelHeader;
import com.bob.db.util.excel.ExcelTextLength;
import com.bob.db.util.excel.RegexValidate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPositionsExcelModel {

//    @ExcelHeader("Requisition Title")
//    @ExcelDropdown(masterClass = JobRequisitionsEntity.class, displayField = "requisitionTitle")
//    private UUID requisitionId;

    @ExcelHeader(value = "Master Position Code",isMandatory = true)
    @ExcelDropdown(masterClass = MasterPositionsEntity.class, displayField = "positionName")
    private UUID masterPositionId;

    @ExcelHeader(value = "Department Name",isMandatory = true)
    @ExcelDropdown(masterClass = DepartmentsEntity.class, displayField = "departmentName")
    private UUID deptId;

    @ExcelHeader(value = "Total Vacancies",isMandatory = true)
    private Integer totalVacancies;

    @ExcelHeader(value = "Eligibility Age Min",isMandatory = true)
    private Integer eligibilityAgeMin;

    @ExcelHeader(value = "Eligibility Age Max",isMandatory = true)
    private Integer eligibilityAgeMax;

    @ExcelHeader(value = "Employment Type",isMandatory = true)
    @ExcelDropdown(masterClass = EmployementTypesEntity.class, displayField = "typeName")
    private UUID employmentType;

//    @ExcelHeader("Cibil Score")
//    private BigDecimal cibilScore;

    @ExcelHeader(value = "Job Grade Code",isMandatory = true)
    @ExcelDropdown(masterClass = JobGradeEntity.class, displayField = "jobGradeCode")
    private UUID gradeId;

    @ExcelHeader(value = "Enable Location Preferences",isMandatory = true)
    private Boolean isLocationPreferenceEnabled;

    @ExcelHeader(value = "Mandatory Experience Months",isMandatory = true)
    private Integer mandatoryExperienceMonths;

    @ExcelHeader("Preferred Experience Months")
    private Integer preferredExperienceMonths;

    @ExcelHeader(value = "Mandatory Experience Text",isMandatory = true)
    @RegexValidate(regex = RegexPattern.ALPHA_NUMERIC_SPACE_PUNCTUATION,message = "Only letters, numbers, spaces and . , - ( ) & : ; / are allowed")
    @ExcelTextLength(max = 2000)
    private String mandatoryExperience;

    @ExcelHeader("Preferred Experience Text")
    @RegexValidate(regex = RegexPattern.ALPHA_NUMERIC_SPACE_PUNCTUATION,message = "Only letters, numbers, spaces and . , - ( ) & : ; / are allowed")
    @ExcelTextLength(max = 2000)
    private String preferredExperience;

//    @ExcelHeader("Mandatory Education Text")
//    private String mandatoryEducation;

//    @ExcelHeader("Preferred Education Text")
//    private String preferredEducation;

    @ExcelHeader(value = "Roles and Responsibilities",isMandatory = true)
    @ExcelTextLength(max = DBConstants.EXCEL_MAX_CELL_LENGTH)
    @RegexValidate(regex = RegexPattern.ALPHA_NUMERIC_SPACE_PUNCTUATION,message = "Only letters, numbers, spaces and . , - ( ) & : ; / are allowed")
    private String rolesResponsibilities;


    @ExcelHeader("Contract Years")
    private Integer contractYears;

// DO NOT REMOVE keep this commented code until confirmation
//    @ExcelHeader("Approved by email id")
//    private String approvedByEmailId;

//    @ExcelHeader("Approved On")
//    private LocalDate approvedOn;

//    @ExcelHeader(value = "Medical Required",isMandatory = true)
//    private Boolean isMedicalRequired;

// DO NOT REMOVE keep this commented code until confirmation
//    private List<PositionRequiredDocumentsDTO> positionRequiredDocuments;

//    private List<PositionStateDistributionDTO> positionStateDistributions;

//    private List<PositionCategoryNationalDistributionDTO> positionCategoryNationalDistributions;
}
