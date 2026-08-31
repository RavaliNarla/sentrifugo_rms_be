package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "candidate_embeddings", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE recruitment.candidate_embeddings SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CandidateEmbeddingsEntity extends BaseEntity<UUID> {

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "application_id")
    private UUID applicationId;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "experience_embedding", columnDefinition = "vector(1536)")
    private float[] experienceEmbedding;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "education_embedding", columnDefinition = "vector(1536)")
    private float[] educationEmbedding;

    @Column(name = "experience_score", precision = 5, scale = 2)
    private BigDecimal experienceScore;

    @Column(name = "education_score", precision = 5, scale = 2)
    private BigDecimal educationScore;

    @Column(name = "experience_embedding_raw_text", columnDefinition = "text")
    private String experienceEmbeddingRawText;

    @Column(name = "education_embedding_raw_text", columnDefinition = "text")
    private String educationEmbeddingRawText;

}

