package com.bob.db.entity;


import com.bob.db.enums.CompensationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "candidate_compensation", schema = "recruitment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE recruitment.candidate_compensation SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CandidateCompensationEntity extends BaseEntity<UUID> {

    @Column(name = "candidate_id")
    private UUID candidateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", referencedColumnName = "candidate_id", insertable = false, updatable = false)
    private CandidateProfileEntity candidateProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", referencedColumnName = "id", nullable = false)
    private CandidateApplicationsEntity application;

    @Column(name = "interview_schedule_id")
    private UUID interviewScheduleId;

//    @Column(name = "panel_id", nullable = false)
//    private UUID panelId;

    @Column(name = "submit_before_date")
    private LocalDate submitBeforeDate;

    @Column(name = "current_ctc", precision = 10, scale = 2)
    private BigDecimal currentCtc;

    @Column(name = "expected_ctc", precision = 10, scale = 2)
    private BigDecimal expectedCtc;

    @Column(name = "hike", precision = 5, scale = 2)
    private BigDecimal hike;

    @Column(name = "agreed_ctc", precision = 10, scale = 2)
    private BigDecimal agreedCtc;

    @Column(name = "fixed_pay", precision = 10, scale = 2)
    private BigDecimal fixedPay;

    @Column(name = "variable_pay", precision = 10, scale = 2)
    private BigDecimal variablePay;

    @Column(name = "joining_bonus", precision = 10, scale = 2)
    private BigDecimal joiningBonus;

    @Column(name = "recruiter_comments")
    private String recruiterComments;

    @Column(name = "panel_comments")
    private String panelComments;

    @Enumerated(EnumType.STRING)
    @Column(name = "compensation_status", length = 50)
    @Builder.Default
    private CompensationStatus compensationStatus=CompensationStatus.NEW;
}