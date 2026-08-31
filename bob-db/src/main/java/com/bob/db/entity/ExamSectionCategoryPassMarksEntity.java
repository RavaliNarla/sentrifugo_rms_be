package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "exam_section_category_pass_marks", schema = "recruitment")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.exam_section_category_pass_marks SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class ExamSectionCategoryPassMarksEntity extends BaseEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_section_id", nullable = false)
    private ExamSectionPassMarksEntity examSection;


    @Column(name = "state_id")
    private UUID stateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ReservationCategoriesEntity category;

    @Column(name = "pass_mark", nullable = false)
    private Integer passMark;
}
