package com.bob.db.entity;

import com.bob.db.enums.InterviewSchedulingApprovalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interview_schedule_staging",schema = "candidate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE candidate.interview_schedule_staging SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class InterviewScheduleStagingEntity extends BaseEntity<UUID>{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private CandidateApplicationsEntity application;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "panel_id", nullable = false)
    private UUID panelId;

    @Column(name = "zonal_office_id", nullable = false)
    private UUID zonalOfficeId;

    @Column(name = "interview_start_at", nullable = false)
    private LocalDateTime interviewStartAt;

    @Column(name = "interview_end_at", nullable = false)
    private LocalDateTime interviewEndAt;

    @Column(name = "interview_duration_minutes")
    @Builder.Default
    private Integer interviewDurationMinutes = 15;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_scheduling_status")
    @Builder.Default
    private InterviewSchedulingApprovalStatus interviewSchedulingApprovalStatus = InterviewSchedulingApprovalStatus.L1_PENDING;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "is_rescheduled")
    @Builder.Default
    private boolean rescheduled=false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", referencedColumnName = "candidate_id", insertable = false, updatable = false)
    private CandidateProfileEntity candidateProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zonal_office_id", referencedColumnName = "id", insertable = false, updatable = false)
    private InterviewCentresEntity interviewCentre;
}
