package com.bob.db.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_positions_history", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.job_positions_history SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class JobPositionsHistoryEntity extends BaseEntity<UUID> {

    @Column(name = "job_position_id", nullable = false)
    private UUID jobPositionId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "action_type", nullable = false, length = 10)
    private String actionType;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_date")
    private LocalDateTime changedDate;

    @Column(name = "requisition_id")
    private UUID requisitionId;

    @Column(name = "master_position_id")
    private UUID masterPositionId;

    @Column(name = "dept_id")
    private UUID deptId;

    @Column(name = "total_vacancies")
    private Integer totalVacancies;

    @Column(name = "eligibility_age_min")
    private Integer eligibilityAgeMin;

    @Column(name = "eligibility_age_max")
    private Integer eligibilityAgeMax;

    @Column(name = "employment_type")
    private UUID employmentType;

    @Column(name = "cibil_score")
    private BigDecimal cibilScore;

    @Column(name = "grade_id")
    private UUID gradeId;

    @Column(name = "mandatory_education", columnDefinition = "text")
    private String mandatoryEducation;

    @Column(name = "preferred_education", columnDefinition = "text")
    private String preferredEducation;

    @Column(name = "mandatory_experience", columnDefinition = "text")
    private String mandatoryExperience;

    @Column(name = "preferred_experience", columnDefinition = "text")
    private String preferredExperience;

    @Column(name = "roles_responsibilities", columnDefinition = "text")
    private String rolesResponsibilities;

    @Column(name = "position_status", length = 20)
    private String positionStatus;

    @Column(name = "is_location_preference_enabled")
    private Boolean isLocationPreferenceEnabled;

    @Column(name = "is_location_wise")
    private Boolean isLocationWise;

    @Column(name = "total_experience", precision = 5, scale = 2)
    private BigDecimal totalExperience;

    @Column(name = "contract_years")
    private Integer contractYears;

    @Column(name = "mandatory_experience_months")
    private Integer mandatoryExperienceMonths;

    @Column(name = "preferred_experience_months")
    private Integer preferredExperienceMonths;

    @Column(name = "indent_path", columnDefinition = "text")
    private String indentPath;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_on")
    private LocalDate approvedOn;

    @Column(name = "is_medical_required")
    private Boolean isMedicalRequired;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mandatory_edu_rules_json", columnDefinition = "jsonb")
    private JsonNode mandatoryEduRulesJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_edu_rules_json", columnDefinition = "jsonb")
    private JsonNode preferredEduRulesJson;

    @Column(name = "indent_name")
    private String indentName;

    @Column(name = "indent_others")
    private String indentOthers;

    @Column(name = "cutoff_date")
    private LocalDate cutoffDate;

    @Column(name = "is_mandatory_exp_months_edu_wise")
    private Boolean isMandatoryExpMonthsEduWise;

    @Column(name = "is_preferred_exp_months_edu_wise")
    private Boolean isPreferredExpMonthsEduWise;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mandatory_exp_months_edu_wise", columnDefinition = "jsonb")
    private JsonNode mandatoryExpMonthsEduWise;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_exp_months_edu_wise", columnDefinition = "jsonb")
    private JsonNode preferredExpMonthsEduWise;

    @Column(name = "is_proficient_in_local_language")
    private Boolean isProficientInLocalLanguage;

    @Column(name = "is_age_rel_wds_women")
    private Boolean isAgeRelWdsWomen;

    @Column(name = "is_age_rel_riot_victim_family")
    private Boolean isAgeRelRiotVictimFamily;

    @Column(name = "is_intermediate_required")
    @Builder.Default
    private Boolean isIntermediateRequired = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dynamic_fields", columnDefinition = "jsonb")
    private JsonNode dynamicFields;
}
