package com.sentrifugo.rms.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Per candidate+round schedule snapshot so Panel Management / history survive when
 * interview_schedule is overwritten on Schedule Next Round.
 */
@Entity
@Table(name = "interview_round_meta", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE recruitment.interview_round_meta SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class InterviewRoundMetaEntity extends BaseEntity<UUID> {

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "round", nullable = false)
    private Integer round;

    @Column(name = "round_name", length = 120)
    private String roundName;

    @Column(name = "panel_id")
    private UUID panelId;

    @Column(name = "interview_date")
    private LocalDate interviewDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;
}
