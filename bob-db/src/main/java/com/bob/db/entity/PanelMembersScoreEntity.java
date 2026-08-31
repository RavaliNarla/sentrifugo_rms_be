package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "panel_member_scores", schema = "recruitment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE recruitment.panel_member_scores SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PanelMembersScoreEntity extends BaseEntity<UUID> {

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "scheduled_interview_id", nullable = false)
    private UUID scheduledInterviewId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "panel_id", nullable = false)
    private UUID panelId;

    @Column(name = "panel_member_id", nullable = false)
    private UUID panelMemberId;

    @Column(name = "panel_score", precision = 5, scale = 2)
    private BigDecimal panelScore;

    @Column(name = "panel_comments", columnDefinition = "text")
    private String panelComments;

    @Column(name = "interview_center_id")
    private UUID interviewCenterId;

    @Column(name = "is_absent", nullable = false)
    @Builder.Default
    private Boolean isAbsent = false;
}
