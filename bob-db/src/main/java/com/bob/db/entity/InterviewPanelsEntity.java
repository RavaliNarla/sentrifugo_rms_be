package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "interview_panels" ,schema = "recruitment")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewPanelsEntity  extends BaseEntity<UUID>{

    @Column(name = "panel_name", length = 200, nullable = false)
    private String panelName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "committee_id", referencedColumnName = "id")
    private InterviewCommitteeEntity committee;

    @OneToMany(mappedBy = "panel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewPanelMembersEntity> panelMembers = new ArrayList<>();
}
