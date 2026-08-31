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
@Table(name = "interview_schedule_staging_history",schema = "candidate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE candidate.interview_schedule_staging_history SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class InterviewScheduleStagingHistoryEntity extends BaseEntity<UUID> {

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

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

    @Column(name = "batch_id")
    private UUID batchId;
}
