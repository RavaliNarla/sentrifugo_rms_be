package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "exam_section", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.exam_section SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class ExamSectionPassMarksEntity extends BaseEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_config_id", nullable = false)
    private WrittenExamConfigurationEntity examConfig;

    @Column(name = "section_number", nullable = false)
    private Integer sectionNumber;

    @Column(name = "section_name")
    private String sectionName;

    @Column(name = "is_ranking_enabled")
    private Boolean isRankingEnabled;

    @Column(name = "section_total_marks")
    private Integer sectionTotalMarks;

    @Column(name = "is_statewise")
    private Boolean isStatewise;
}
