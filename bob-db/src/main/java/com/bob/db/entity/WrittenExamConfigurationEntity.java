package com.bob.db.entity;

import com.bob.db.enums.WrittenExamConfigurationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "written_exam_configuration", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.written_exam_configuration SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class WrittenExamConfigurationEntity extends BaseEntity<UUID> {

    public static final String ENTITY_TYPE = "written_exam_configuration";

    @Column(name = "position_id", nullable = false, unique = true)
    private UUID positionId;

    @Column(name = "exam_name", nullable = false)
    private String examName;

    @Column(name = "total_marks")
    private Integer totalMarks;

    @Column(name = "number_of_sections")
    private Integer numberOfSections;

    @Column(name = "marks_per_section")
    private Integer marksPerSection;

    @Column(name = "written_exam_weightage", precision = 5, scale = 2)
    private BigDecimal writtenExamWeightage;

    @Column(name = "interview_weightage", precision = 5, scale = 2)
    private BigDecimal interviewWeightage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private WrittenExamConfigurationStatus status;

    @Column(name = "comments")
    private String comments;

    @Column(name ="is_frozen")
    @Builder.Default
    private Boolean isFrozen = false;
}
