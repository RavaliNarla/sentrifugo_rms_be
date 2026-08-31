package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "interview_panel_members" ,schema = "recruitment")
public class InterviewPanelMembersEntity  extends BaseEntity<UUID>{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "panel_id", nullable = false)
    private InterviewPanelsEntity panel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "panel_member_id", nullable = false)
    private UserEntity panelMember;
}
