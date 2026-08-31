package com.bob.db.entity;

import com.bob.db.enums.PositionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_positions", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.job_positions SET is_active = false, position_status = 'INACTIVE' WHERE id = ?")
@Where(clause = "is_active = true")
public class JobPositionsEntity extends BaseEntity<UUID> {

    @Column(name = "requisition_id", nullable = false)
    private UUID requisitionId;

    @Column(name = "master_position_id", nullable = false)
    private UUID masterPositionId;

    @Column(name = "dept_id")
    private UUID deptId;

    @Column(name = "parent_position_id")
    private UUID parentPositionId;

    @Column(name = "total_vacancies")
    private Integer totalVacancies;

    @Column(name = "remaining_total_vacancies")
    private Integer remainingTotalVacancies;

    @Column(name = "offers_sent", columnDefinition = "int4 DEFAULT 0")
    @Builder.Default
    private Integer offersSent = 0;

    @Column(name = "offers_accepted", columnDefinition = "int4 DEFAULT 0")
    @Builder.Default
    private Integer offersAccepted = 0;

    @Column(name = "is_hiring_completed")
    @Builder.Default
    private Boolean isHiringCompleted = false;

    @Column(name = "eligibility_age_min")
    private Integer eligibilityAgeMin;

    @Column(name = "eligibility_age_max")
    private Integer eligibilityAgeMax;

    @Column(name = "employment_type")
    private UUID employmentType;

    @Column(name = "cibil_score")
    private BigDecimal cibilScore;

    @Column(name = "grade_id", nullable = false)
    private UUID gradeId;

    @Column(name = "mandatory_education", columnDefinition = "TEXT")
    private String mandatoryEducation;

    @Column(name = "preferred_education", columnDefinition = "TEXT")
    private String preferredEducation;


    @Column(name = "roles_responsibilities", nullable = false, columnDefinition = "TEXT")
    private String rolesResponsibilities;

    @Enumerated(EnumType.STRING)
    @Column(name = "position_status", length = 20)
    @Builder.Default
    private PositionStatus positionStatus = PositionStatus.DRAFT;

    @Column(name = "contract_years", columnDefinition = "int2")
    private Integer contractYears;

    @Column(name = "is_location_preference_enabled", nullable = false)
    @Builder.Default
    private Boolean isLocationPreferenceEnabled = false;

    @Column(name = "is_location_wise", nullable = false)
    @Builder.Default
    private Boolean isLocationWise = false;

    @Column(name = "total_experience", precision = 5, scale = 2)
    private BigDecimal totalExperience;

    @Column(name = "mandatory_experience", nullable = false, columnDefinition = "TEXT")
    private String mandatoryExperience;

    @Column(name = "mandatory_experience_months", columnDefinition = "int2 DEFAULT 0")
    private Integer mandatoryExperienceMonths;

    @Column(name = "preferred_experience", nullable = false, columnDefinition = "TEXT")
    private String preferredExperience;

    @Column(name = "preferred_experience_months", columnDefinition = "int2 DEFAULT 0")
    private Integer preferredExperienceMonths;

    @Column(name = "indent_path", columnDefinition = "TEXT")
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

    @Column(name = "indent_name", length = 100)
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

//    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "jobPosition", orphanRemoval = true)
//    private List<PositionRequiredDocumentsEntity> positionRequiredDocuments = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "jobPosition", orphanRemoval = true)
    private List<PositionStateDistributionEntity> positionStateDistributions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "jobPosition", orphanRemoval = true)
    private List<PositionCategoryNationalDistributionEntity> positionCategoryNationalDistributions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "jobPosition", orphanRemoval = true)
    private List<JobPositionExclusionsEntity> jobPositionExclusion = new ArrayList<>();


    @Column(name = "is_proficient_in_local_language")
    @Builder.Default
    private Boolean isProficientInLocalLanguage = false;

    // Age relaxation applicable to widowed, divorced and judicially separated women.
    @Column(name = "is_age_rel_wds_women")
    @Builder.Default
    private Boolean isAgeRelWdsWomen = false;

    // Age relaxation applicable to 1984 riots victim family.
    @Column(name = "is_age_rel_riot_victim_family")
    @Builder.Default
    private Boolean isAgeRelRiotVictimFamily = false;

    @Column(name = "is_intermediate_required")
    @Builder.Default
    private Boolean isIntermediateRequired = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dynamic_fields", columnDefinition = "jsonb")
    private JsonNode dynamicFields;

//    public void clearPositionRequiredDocuments() {
//        if (this.positionRequiredDocuments != null) {
//            for (PositionRequiredDocumentsEntity doc : new ArrayList<>(this.positionRequiredDocuments)) {
//                doc.setJobPosition(null);
//            }
//            this.positionRequiredDocuments.clear();
//        }
//    }

    public void clearPositionStateDistributions() {
        if (this.positionStateDistributions != null) {
            for (PositionStateDistributionEntity dist : new ArrayList<>(this.positionStateDistributions)) {
                dist.clearPositionCategoryDistributions();
                dist.setJobPosition(null);
            }
            this.positionStateDistributions.clear();
        }
    }

    public void clearPositionCategoryNationalDistributions() {
        if (this.positionCategoryNationalDistributions != null) {
            for (PositionCategoryNationalDistributionEntity doc : new ArrayList<>(this.positionCategoryNationalDistributions)) {
                doc.setJobPosition(null);
            }
            this.positionCategoryNationalDistributions.clear();
        }
    }

    public void clearJobPositionExclusions() {
        if (this.jobPositionExclusion != null) {
            for (JobPositionExclusionsEntity exclusion : new ArrayList<>(this.jobPositionExclusion)) {
                exclusion.setJobPosition(null);
            }
            this.jobPositionExclusion.clear();
        }
    }

    // Explicit getters to ensure collections are never null (prevents NPE when addAll is called)
    // These override the Lombok @Data generated getters to provide defensive null-initialization
    public List<PositionStateDistributionEntity> getPositionStateDistributions() {
        if (this.positionStateDistributions == null) {
            this.positionStateDistributions = new ArrayList<>();
        }
        return this.positionStateDistributions;
    }

    public List<PositionCategoryNationalDistributionEntity> getPositionCategoryNationalDistributions() {
        if (this.positionCategoryNationalDistributions == null) {
            this.positionCategoryNationalDistributions = new ArrayList<>();
        }
        return this.positionCategoryNationalDistributions;
    }

    public List<JobPositionExclusionsEntity> getJobPositionExclusion() {
        if (this.jobPositionExclusion == null) {
            this.jobPositionExclusion = new ArrayList<>();
        }
        return this.jobPositionExclusion;
    }

}

