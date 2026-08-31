package com.bob.db.entity;

import com.bob.db.enums.ScreeningCommentUserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "screening_comments", schema = "candidate")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE candidate.screening_comments SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class ScreeningCommentsEntity extends BaseEntity<UUID> {

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false, length = 30)
    private ScreeningCommentUserRole userRole;

    @Column(name = "comment_text", nullable = false, columnDefinition = "text")
    private String commentText;
}

