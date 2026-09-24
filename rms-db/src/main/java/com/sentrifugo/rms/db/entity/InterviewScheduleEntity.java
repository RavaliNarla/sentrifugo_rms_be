package com.sentrifugo.rms.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "interview_schedule", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE recruitment.interview_schedule SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class InterviewScheduleEntity extends BaseEntity<UUID> {

    @Column(name = "candidate_id", nullable = false, unique = true)
    private UUID candidateId;

    @Column(name = "panel_id", nullable = false)
    private UUID panelId;

    @Column(name = "interview_date", nullable = false)
    private LocalDate interviewDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "duration_minutes")
    @Builder.Default
    private Integer durationMinutes = 30;

    /** Interview round number (1 = first schedule; increments on Schedule Next Round). */
    @Column(name = "round", nullable = false)
    @Builder.Default
    private Integer round = 1;

    /** Token for Accept / Decline links in the candidate invite email. */
    @Column(name = "accept_token", unique = true)
    private UUID acceptToken;

    /** Prior accept tokens (comma-separated) so old email links resolve as superseded. */
    @Column(name = "superseded_tokens", length = 4000)
    private String supersededTokens;

    @Column(name = "invite_sent_at")
    private LocalDateTime inviteSentAt;

    @Column(name = "invite_responded_at")
    private LocalDateTime inviteRespondedAt;
}
