package com.bob.db.entity;

import java.util.UUID;

import com.bob.db.entity.BaseEntity;
import com.bob.db.enums.InterviewSchedulingStatus;
import com.bob.db.enums.LptStatus;
import com.bob.db.enums.MailSendStatus;
import com.bob.db.enums.ZonalVerificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interview_schedule", schema = "candidate")
@NoArgsConstructor
@AllArgsConstructor
@Data
@SuperBuilder
@SQLDelete(sql = "UPDATE candidate.interview_schedule SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class InterviewScheduleEntity extends BaseEntity<UUID> {

    public static final String ENTITY_TYPE = "interview_schedule";

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "panel_id", nullable = false)
    private UUID panelId;

    @Column(name = "interview_start_at", nullable = false)
    private LocalDateTime interviewStartAt;

    @Column(name = "interview_end_at")
    private LocalDateTime interviewEndAt;

    @Column(name = "interview_duration_minutes", nullable = false)
    @Builder.Default
    private Integer interviewDurationMinutes = 15;

    @Column(name = "meeting_link")
    private String meetingLink;

    @Column(name = "zonal_office_id")
    private UUID zonalOfficeId;

    @Column(name = "final_score", precision = 5, scale = 2)
    private BigDecimal finalScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_status", length = 50)
    @Builder.Default
    private InterviewSchedulingStatus interviewStatus=InterviewSchedulingStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(name = "zonal_verification_status", length = 50)
    @Builder.Default
    private ZonalVerificationStatus zonalVerificationStatus = ZonalVerificationStatus.PENDING;

    @Column(name = "zonal_submit_before_date")
    private LocalDate zonalSubmitBeforeDate;

    @Column(name = "zonal_hr_comments", columnDefinition = "text")
    private String zonalHrComments;

    @Column(name = "lpt_required")
    private Boolean lptRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "lpt_status", length = 50)
    private LptStatus lptStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "mail_send_status", length = 20)
    @Builder.Default
    private MailSendStatus mailSendStatus = MailSendStatus.PENDING;

    @Column(name = "mail_sent_on")
    private LocalDateTime mailSentOn;

    @Column(name = "mail_comments")
    private String mailComments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zonal_office_id", referencedColumnName = "id", insertable = false, updatable = false)
    private InterviewCentresEntity interviewCentre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "panel_id", referencedColumnName = "id", insertable = false, updatable = false)
    private InterviewPanelsEntity interviewPanels;

}