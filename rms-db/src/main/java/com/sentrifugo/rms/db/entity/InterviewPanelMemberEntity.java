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

import java.util.UUID;

@Entity
@Table(name = "interview_panel_members", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE recruitment.interview_panel_members SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class InterviewPanelMemberEntity extends BaseEntity<UUID> {

    @Column(name = "panel_id", nullable = false)
    private UUID panelId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
