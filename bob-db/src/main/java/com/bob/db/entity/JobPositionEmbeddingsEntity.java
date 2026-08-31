package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_position_embeddings", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.job_position_embeddings SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class JobPositionEmbeddingsEntity extends BaseEntity<UUID> {

    @Column(name = "position_id", nullable = false)
    private UUID positionId;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "experience_embedding", columnDefinition = "vector")
    private float[] experienceEmbedding;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "education_embedding", columnDefinition = "vector")
    private float[] educationEmbedding;

    @Column(name = "experience_embedding_raw_text", columnDefinition = "text")
    private String experienceEmbeddingRawText;

    @Column(name = "education_embedding_raw_text", columnDefinition = "text")
    private String educationEmbeddingRawText;


}

