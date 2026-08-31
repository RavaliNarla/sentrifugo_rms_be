package com.bob.db.entity;

import com.bob.db.enums.CandidateScreeningCriteriaStatus;
import com.bob.db.enums.CandidateScreeningShortlistedStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "candidate_screening", schema = "candidate")
@SQLDelete(sql = "UPDATE candidate.candidate_screening SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CandidateScreeningEntity extends BaseEntity<UUID> {

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_work_criteria_met")
    @Builder.Default
    private CandidateScreeningCriteriaStatus isWorkCriteriaMet = CandidateScreeningCriteriaStatus.DEFAULT;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_age_criteria_met")
    @Builder.Default
    private CandidateScreeningCriteriaStatus isAgeCriteriaMet = CandidateScreeningCriteriaStatus.DEFAULT;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_education_criteria_met")
    @Builder.Default
    private CandidateScreeningCriteriaStatus isEducationCriteriaMet  = CandidateScreeningCriteriaStatus.DEFAULT;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_shortlisted")
    @Builder.Default
    private CandidateScreeningShortlistedStatus isShortlisted = CandidateScreeningShortlistedStatus.DEFAULT;

    @Column(name = "is_eligible")
    @Builder.Default
    private Boolean isEligible = false;

    @Column(name = "work_criteria_remark", columnDefinition = "text")
    private String workCriteriaRemark;

    @Column(name = "age_criteria_remark", columnDefinition = "text")
    private String ageCriteriaRemark;

    @Column(name = "education_criteria_remark", columnDefinition = "text")
    private String educationCriteriaRemark;

    @Column(name = "final_screening_remark", columnDefinition = "text")
    private String finalScreeningRemark;

    @Column(name = "submit_before_date")
    private LocalDate submitBeforeDate;

    @Column(name = "is_screening_completed")
    @Builder.Default
    private Boolean isScreeningCompleted = false;



}