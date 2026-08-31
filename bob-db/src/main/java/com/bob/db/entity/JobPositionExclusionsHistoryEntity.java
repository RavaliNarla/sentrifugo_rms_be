package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "job_position_exclusions_history", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.job_position_exclusions_history SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class JobPositionExclusionsHistoryEntity extends BaseEntity<UUID> {

    @Column(name = "job_position_exclusions_id", nullable = false)
    private UUID jobPositionExclusionsId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "action_type", nullable = false, length = 10)
    private String actionType;

    @Column(name = "position_id", nullable = false)
    private UUID positionId;

    @Column(name = "exclusion_id", nullable = false)
    private UUID exclusionId;

    @Column(name = "is_excluded", nullable = false)
    @Builder.Default
    private Boolean isExcluded =false;
}
