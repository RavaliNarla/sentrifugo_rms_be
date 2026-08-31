package com.bob.db.entity;

import com.bob.db.enums.ExamQualificationStatus;
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
@Table(name = "candidate_written_exam_marks", schema = "candidate")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE candidate.candidate_written_exam_marks SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CandidateWrittenExamMarksEntity extends BaseEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidatesEntity candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private CandidateApplicationsEntity application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false)
    private JobPositionsEntity position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_config_id", nullable = false)
    private WrittenExamConfigurationEntity examConfig;

    @Column(name = "roll_number", length = 50)
    private String rollNumber;

    @Column(name = "total_marks_obtained", precision = 10, scale = 4)
    private BigDecimal totalMarksObtained;

    @Column(name = "ranking_marks_obtained", precision = 10, scale = 4)
    private BigDecimal rankingMarksObtained;

    @Column(name = "normalized_score", precision = 10, scale = 4)
    private BigDecimal normalizedScore;

    @Column(name = "final_weighted_score", precision = 10, scale = 4)
    private BigDecimal finalWeightedScore;

    @Column(name = "rank_number")
    private Integer rankNumber;

    @Column(name = "is_passed")
    private Boolean isPassed;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status")
    private ExamQualificationStatus status = ExamQualificationStatus.NOT_MARKED;

    @Column(name = "remarks", columnDefinition = "text")
    private String remarks;
}
