package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.AttributeOverride;
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
@Table(name = "position_category_national_distribution_history", schema = "recruitment")
@AttributeOverride(name = "id", column = @Column(name = "history_id", updatable = false, nullable = false))
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.position_category_national_distribution_history SET is_active = false WHERE history_id = ?")
@Where(clause = "is_active = true")
public class PositionCategoryNationalDistributionHistoryEntity extends BaseEntity<UUID> {

    @Column(name = "position_cat_nat_dist_id", nullable = false)
    private UUID positionCatNatDistId;

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

    @Column(name = "reservation_category_id")
    private UUID reservationCategoryId;

    @Column(name = "disability_category_id")
    private UUID disabilityCategoryId;

    @Column(name = "vacancy_count")
    private Integer vacancyCount;

    @Column(name = "is_disability")
    private Boolean isDisability;
}
