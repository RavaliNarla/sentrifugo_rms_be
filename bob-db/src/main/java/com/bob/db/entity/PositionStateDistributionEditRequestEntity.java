package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "position_state_distribution_edit_requests", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.position_state_distribution_edit_requests SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PositionStateDistributionEditRequestEntity extends BaseEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_edit_position_id", nullable = false)
    private JobPositionEditRequestEntity jobEditPosition;

    @Column(name = "state_id", nullable = false)
    private UUID stateId;

    @Column(name = "city_id")
    private UUID cityId;

    @Column(name = "total_vacancies", nullable = false)
    private Integer totalVacancies;

    @Column(name = "local_language", length = 100)
    private String localLanguage;

    @OneToMany(mappedBy = "stateDistribution", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PositionCategoryDistributionEditRequestEntity> positionCategoryDistributionEditRequests = new ArrayList<>();
}
