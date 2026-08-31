package com.bob.db.entity;

import com.bob.db.enums.PositionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_position_edit_requests", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.job_position_edit_requests SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class JobPositionEditRequestEntity extends BaseEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_edit_requisition_id", nullable = false)
    private JobRequisitionEditRequestEntity jobEditRequisition;

    @Column(name = "parent_position_id", nullable = false)
    private UUID parentPositionId;

    @Column(name = "master_position_id", nullable = false)
    private UUID masterPositionId;

    @Column(name = "dept_id")
    private UUID deptId;

    @Column(name = "total_vacancies")
    private Integer totalVacancies;

    @Column(name = "eligibility_age_min")
    private Integer eligibilityAgeMin;

    @Column(name = "eligibility_age_max")
    private Integer eligibilityAgeMax;

    @Column(name = "employment_type", nullable = false)
    private UUID employmentType;

    @Column(name = "cibil_score")
    private BigDecimal cibilScore;

    @Column(name = "grade_id", nullable = false)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "position_status", length = 20)
    private PositionStatus positionStatus;

    @Column(name = "is_location_preference_enabled", nullable = false)
    @Builder.Default
    private Boolean isLocationPreferenceEnabled = false;

    @Column(name = "is_location_wise", nullable = false)
    @Builder.Default
    private Boolean isLocationWise = false;

    @Column(name = "total_experience", precision = 5, scale = 2)
    private BigDecimal totalExperience;

    @Column(name = "contract_years")
    private Integer contractYears;

    @Column(name = "mandatory_experience_months")
    @Builder.Default
    private Integer mandatoryExperienceMonths = 0;

    @Column(name = "preferred_experience_months")
    @Builder.Default
    private Integer preferredExperienceMonths = 0;

    @Column(name = "indent_path", columnDefinition = "text")
    private String indentPath;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_on")
    private LocalDate approvedOn;

    @Column(name = "is_medical_required", nullable = false)
    @Builder.Default
    private Boolean isMedicalRequired = false;

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
    @Builder.Default
    private Boolean isMandatoryExpMonthsEduWise = false;

    @Column(name = "is_preferred_exp_months_edu_wise")
    @Builder.Default
    private Boolean isPreferredExpMonthsEduWise = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mandatory_exp_months_edu_wise", columnDefinition = "jsonb")
    private JsonNode mandatoryExpMonthsEduWise;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_exp_months_edu_wise", columnDefinition = "jsonb")
    private JsonNode preferredExpMonthsEduWise;

    @Column(name = "is_proficient_in_local_language")
    @Builder.Default
    private Boolean isProficientInLocalLanguage = false;

    @Column(name = "is_age_rel_wds_women")
    @Builder.Default
    private Boolean isAgeRelWdsWomen = false;

    @Column(name = "is_age_rel_riot_victim_family")
    @Builder.Default
    private Boolean isAgeRelRiotVictimFamily = false;

    @OneToMany(mappedBy = "jobEditPosition", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PositionStateDistributionEditRequestEntity> positionStateDistributionEditRequests = new ArrayList<>();

    @OneToMany(mappedBy = "jobEditPosition", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PositionCategoryNationalDistributionEditRequestEntity> positionCategoryNationalDistributionEditRequests = new ArrayList<>();

    @OneToMany(mappedBy = "jobEditPosition", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<JobPositionExclusionsEditRequestEntity> JobPositionExclusionsEditRequestEntity = new ArrayList<>();

    @Column(name = "is_intermediate_required")
    @Builder.Default
    private Boolean isIntermediateRequired = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dynamic_fields", columnDefinition = "jsonb")
    private JsonNode dynamicFields;
}
