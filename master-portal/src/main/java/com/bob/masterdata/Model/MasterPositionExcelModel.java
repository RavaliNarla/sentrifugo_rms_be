    package com.bob.masterdata.Model;

    import com.bob.db.entity.DepartmentsEntity;
    import com.bob.db.entity.JobGradeEntity;
    import com.bob.db.enums.RegexPattern;
    import com.bob.db.util.excel.ExcelDropdown;
    import com.bob.db.util.excel.ExcelHeader;
    import com.bob.db.util.excel.RegexValidate;
    import com.fasterxml.jackson.annotation.JsonProperty;
    import lombok.Data;

    import java.math.BigDecimal;
    import java.util.UUID;
    @Data
    public class MasterPositionExcelModel {
        @JsonProperty("masterPositionsId")
        private UUID id;


        @ExcelHeader(value = "Position Title",isMandatory = true)
        @RegexValidate(regex = RegexPattern.ALPHA_NUMERIC_SPACE_DASH_AMP ,message = "Field should only contain alphabets, numbers, spaces and special characters[&,-]")
        private String positionName;

        @ExcelDropdown(masterClass = DepartmentsEntity.class, displayField = "departmentName")
        @ExcelHeader(value = "Department",isMandatory = true)
        private UUID deptId;

        @ExcelHeader(value = "Minimum Age",isMandatory = true)
        @RegexValidate(regex = RegexPattern.NUMBERS_ONLY ,message = "Field should only contain numbers")
        private String eligibilityAgeMin;

        @ExcelHeader(value = "Maximum Age",isMandatory = true)
        @RegexValidate(regex = RegexPattern.NUMBERS_ONLY ,message = "Field should only contain numbers")
        private String eligibilityAgeMax;



        @ExcelHeader(value = "Job Grade",isMandatory = true)
        @ExcelDropdown(masterClass = JobGradeEntity.class, displayField = "jobGradeCode")
        private UUID gradeId;

    //    private String mandatoryEducation;
    //
    //    private String preferredEducation;

        @ExcelHeader(value = "Mandatory Experience",isMandatory = true)
        @RegexValidate(regex = RegexPattern.TITLE_ALPHANUMERIC_SPACE_PUNCTUATION ,message = "Field should only contain alphabets, numbers, spaces and special characters[.,/,-]")
        private String mandatoryExperience;

        @ExcelHeader(value = "Preferred Experience",isMandatory = true)
        @RegexValidate(regex = RegexPattern.TITLE_ALPHANUMERIC_SPACE_PUNCTUATION ,message = "Field should only contain alphabets, numbers, spaces and special characters[.,/,-]")
        private String preferredExperience;

        @ExcelHeader(value = "Roles and Responsibilities",isMandatory = true)
        @RegexValidate(regex = RegexPattern.TEXTAREA_BASIC ,message = "Field should only contain alphabets, numbers, spaces and special characters[.,/,-]")
        private String rolesResponsibilities;
    }
