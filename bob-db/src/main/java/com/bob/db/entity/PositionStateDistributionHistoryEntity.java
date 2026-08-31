package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "position_state_distribution_history", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.position_state_distribution_history SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PositionStateDistributionHistoryEntity extends BaseEntity<UUID> {

    @Column(name = "position_state_dist_id", nullable = false)
    private UUID positionStateDistId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "action_type", nullable = false, length = 10)
    private String actionType;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_date")
    private LocalDateTime changedDate;

    @Column(name = "position_id")
    private UUID positionId;

    @Column(name = "state_id")
    private UUID stateId;

    @Column(name = "city_id")
    private UUID cityId;

    @Column(name = "total_vacancies")
    private Integer totalVacancies;

    @Column(name = "local_language")
    private String localLanguage;
}
