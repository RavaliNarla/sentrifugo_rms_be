package com.bob.db.entity;

import com.bob.db.enums.PositionPanelStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "position_panels", schema = "recruitment")
@Data
@EqualsAndHashCode(callSuper = true)
public class PositionPanelEntity extends BaseEntity<UUID>{

    public static final String ENTITY_TYPE = "position_panels";

    @ManyToOne
    @JoinColumn(name = "position_id", nullable = false)
    private JobPositionsEntity jobPosition;

    @ManyToOne
    @JoinColumn(name = "panel_id", nullable = false)
    private InterviewPanelsEntity interviewPanel;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PositionPanelStatus positionPanelStatus;

    @Column(name = "comments")
    private String comments;

}
