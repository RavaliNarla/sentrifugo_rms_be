package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.UUID;

@Data
@Entity
@Table(name = "interviews", schema = "recruitment")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.interviews SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class InterviewsEntity  extends BaseEntity<UUID>{

    public static final String ENTITY_TYPE = "interviews";

    @Column(name = "is_panel_interview")
    private Boolean isPanelInterview;

    @Column(name = "phone")
    private String phone;

    @Column(name="location")
    private String location;

    @Column(name = "application_id")
    private UUID applicationId;

    @Column(name = "type")
    private String type;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "time", precision = 4, scale = 2)
    private BigDecimal time;

    @Column(name = "status")
    private String status;

    @Column(name = "interviewer_id")
    private UUID interviewerId;

    @Column(name = "end_time")
    private LocalDateTime endTime;

}
