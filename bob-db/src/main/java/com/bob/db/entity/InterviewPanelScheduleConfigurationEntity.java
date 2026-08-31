package com.bob.db.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interview_panel_schedule_configuration", schema = "candidate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE candidate.interview_panel_schedule_configuration SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class InterviewPanelScheduleConfigurationEntity extends BaseEntity<UUID> {

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "panel_id", nullable = false)
    private UUID panelId;

    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDatetime;

    @Column(name = "end_datetime", nullable = false)
    private LocalDateTime endDatetime;

    @Column(name = "interviews_per_day")
    private Integer interviewsPerDay;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

}