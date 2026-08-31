package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "position_category_distribution", schema = "recruitment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Where(clause = "is_active = true")
public class PositionCategoryDistributionEntity extends BaseEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_distribution_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PositionStateDistributionEntity positionStateDistribution;

    @Column(name = "reservation_category_id")
    private UUID reservationCategoryId;

    @Column(name = "disability_category_id")
    private UUID disabilityCategoryId;

    @Column(name = "vacancy_count", nullable = false)
    private Integer vacancyCount;

    @Column(name = "remaining_vacancy_count")
    private Integer remainingVacancyCount;

    @Column(name = "offers_sent", columnDefinition = "int4 DEFAULT 0")
    @Builder.Default
    private Integer offersSent = 0;

    @Column(name = "offers_accepted", columnDefinition = "int4 DEFAULT 0")
    @Builder.Default
    private Integer offersAccepted = 0;

    @Column(name = "is_disability", columnDefinition = "boolean DEFAULT false")
    @Builder.Default
    private Boolean isDisability = false;

}
