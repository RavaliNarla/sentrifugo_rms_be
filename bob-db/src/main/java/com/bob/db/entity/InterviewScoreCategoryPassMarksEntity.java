package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "interview_score_category_pass_marks", schema = "common")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE common.interview_score_category_pass_marks SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class InterviewScoreCategoryPassMarksEntity extends BaseEntity<UUID> {

    @Column(name = "reservation_category_id", nullable = false)
    private UUID reservationCategoryId;

    @Column(name = "required_pass_marks", nullable = false)
    private Short requiredPassMarks;
}
