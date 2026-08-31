package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "position_category_distribution_edit_requests", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.position_category_distribution_edit_requests SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PositionCategoryDistributionEditRequestEntity extends BaseEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_edit_position_id")
    private JobPositionEditRequestEntity jobEditPosition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_distribution_id")
    private PositionStateDistributionEditRequestEntity stateDistribution;

    @Column(name = "reservation_category_id")
    private UUID reservationCategoryId;

    @Column(name = "disability_category_id")
    private UUID disabilityCategoryId;

    @Column(name = "vacancy_count", nullable = false)
    private Integer vacancyCount;

    @Column(name = "is_disability")
    @Builder.Default
    private Boolean isDisability = false;
}
