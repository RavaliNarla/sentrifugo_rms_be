package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "scoring_weightage", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE recruitment.scoring_weightage SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class ScoringWeightageEntity extends BaseEntity<UUID> {

    @Column(name = "education_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal educationScore;

    @Column(name = "experience_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal experienceScore;

    @Column(name = "education_similarity", precision = 5, scale = 2, nullable = false)
    private BigDecimal educationSimilarity;

    @Column(name = "experience_similarity", precision = 5, scale = 2, nullable = false)
    private BigDecimal experienceSimilarity;

}

