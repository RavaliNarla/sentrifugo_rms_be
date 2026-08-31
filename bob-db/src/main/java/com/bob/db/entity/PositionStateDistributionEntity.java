package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "position_state_distribution", schema = "recruitment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Where(clause = "is_active = true")
public class PositionStateDistributionEntity extends BaseEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private JobPositionsEntity jobPosition;

    @Column(name = "state_id", nullable = false)
    private UUID stateId;

    @Column(name = "city_id")
    private UUID cityId;

    @Column(name = "total_vacancies", nullable = false)
    private Integer totalVacancies;

    @Column(name = "remaining_total_vacancies")
    private Integer remainingTotalVacancies;

    @Column(name = "offers_sent", columnDefinition = "int4 DEFAULT 0")
    @Builder.Default
    private Integer offersSent = 0;

    @Column(name = "offers_accepted", columnDefinition = "int4 DEFAULT 0")
    @Builder.Default
    private Integer offersAccepted = 0;

    @Column(name = "local_language", length = 100)
    private String localLanguage;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "positionStateDistribution", orphanRemoval = true)
    private List<PositionCategoryDistributionEntity> positionCategoryDistributions;

    public void clearPositionCategoryDistributions() {
        if (this.positionCategoryDistributions != null) {
            for (PositionCategoryDistributionEntity dist : new ArrayList<>(this.positionCategoryDistributions)) {
                dist.setPositionStateDistribution(null);
            }
            this.positionCategoryDistributions.clear();
        }
    }
}
