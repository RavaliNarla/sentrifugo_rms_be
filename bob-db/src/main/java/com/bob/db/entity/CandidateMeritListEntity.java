package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "candidate_merit_list", schema = "candidate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE candidate.candidate_merit_list SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CandidateMeritListEntity extends BaseEntity<UUID> {

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "position_id", nullable = false)
    private UUID positionId;

    @Column(name = "application_no", length = 100)
    private String applicationNo;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "selected_category_id")
    private UUID selectedCategoryId;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "is_pwd")
    @Builder.Default
    private Boolean isPwd = false;

    @Column(name = "general_eligible")
    @Builder.Default
    private Boolean generalEligible = false;

    @Column(name = "state_id")
    private UUID stateId;

    @Column(name = "city_id")
    private UUID cityId;

    @Column(name = "written_score", precision = 10, scale = 2)
    private BigDecimal writtenScore;

    @Column(name = "interview_score", precision = 10, scale = 2)
    private BigDecimal interviewScore;

    @Column(name = "combined_score", precision = 10, scale = 2)
    private BigDecimal combinedScore;

    @Column(name = "is_qualified")
    @Builder.Default
    private Boolean isQualified = false;

    @Column(name = "is_ex_servicemen")
    @Builder.Default
    private Boolean isExServicemen = false;

    @Column(name = "shortlisted")
    @Builder.Default
    private Boolean shortlisted = false;

    @Column(name = "selected")
    @Builder.Default
    private Boolean selected = false;

    @Column(name = "waitlisted")
    @Builder.Default
    private Boolean waitlisted = false;

    @Column(name = "overall_rank")
    private Integer overallRank;

    @Column(name = "category_rank")
    private Integer categoryRank;

    @Column(name = "selected_against_pwd")
    @Builder.Default
    private Boolean selectedAgainstPwd = false;

    @Column(name = "selected_pwd_category_id")
    private UUID selectedPwdCategoryId;

    @Column(name = "interview_weighted_score")
    private BigDecimal interviewWeightedScore;

    @Column(name = "exam_weighted_score")
    private BigDecimal examWeightedScore;

    @Column(name = "is_ews_seat")
    @Builder.Default
    private Boolean isEWSSeat = false;
}