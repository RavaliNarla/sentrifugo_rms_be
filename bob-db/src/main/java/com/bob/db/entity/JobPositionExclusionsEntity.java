package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "job_position_exclusions", schema = "recruitment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Where(clause = "is_active = true")
public class JobPositionExclusionsEntity extends BaseEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private JobPositionsEntity jobPosition;

    @Column(name = "exclusion_id", nullable = false)
    private UUID exclusionId;

    @Column(name = "is_excluded", nullable = false, columnDefinition = "boolean DEFAULT false")
    private Boolean isExcluded = false;

}

