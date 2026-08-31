package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "candidate_disability_details", schema = "candidate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateDisabilityDetailsEntity extends BaseEntity<UUID> {

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "disability_category_id")
    private UUID disabilityCategoryId;

    @Column(name = "disability_percentage")
    private Short disabilityPercentage;

}

