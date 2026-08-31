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
@Table(name = "candidate_exam_section_marks", schema = "candidate")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE candidate.candidate_exam_section_marks SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CandidateExamSectionMarksEntity extends BaseEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidatesEntity candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private CandidateApplicationsEntity application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_config_id", nullable = false)
    private WrittenExamConfigurationEntity examConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_section_id", nullable = false)
    private ExamSectionPassMarksEntity examSection;

    @Column(name = "section_number", nullable = false)
    private Integer sectionNumber;

    @Column(name = "obtained_marks", precision = 5, scale = 2)
    private BigDecimal obtainedMarks;

    @Column(name = "is_qualified")
    private Boolean isQualified;

    @Column(name = "is_ranking_enabled")
    private Boolean isRankingEnabled;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
