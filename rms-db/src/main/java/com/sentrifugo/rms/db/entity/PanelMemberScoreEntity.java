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

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "panel_member_scores", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE recruitment.panel_member_scores SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PanelMemberScoreEntity extends BaseEntity<UUID> {

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "panel_member_id", nullable = false)
    private UUID panelMemberId;

    /** Rating scale 1-10, pass mark 5 (Section 14 of the requirements doc). */
    @Column(name = "score", precision = 5, scale = 2, nullable = false)
    private BigDecimal score;

    /** Renamed from "Comment" - interviewer must justify the score given. */
    @Column(name = "rationale", columnDefinition = "TEXT")
    private String rationale;

    /** SELECT / REJECT / HOLD. */
    @Column(name = "decision")
    private String decision;
}
