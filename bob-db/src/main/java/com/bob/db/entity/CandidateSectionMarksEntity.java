package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "candidate_section_marks", schema = "candidate")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE candidate.candidate_section_marks SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CandidateSectionMarksEntity extends BaseEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_exam_id", nullable = false)
    private CandidateWrittenExamMarksEntity candidateExam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_section_id", nullable = false)
    private ExamSectionPassMarksEntity examSection;

    @Column(name = "marks_obtained", nullable = false, precision = 10, scale = 4)
    private BigDecimal marksObtained;

    @Column(name = "is_section_passed")
    private Boolean isSectionPassed;

    @Column(name = "is_ranking_enabled")
    private Boolean isRankingEnabled;
}
