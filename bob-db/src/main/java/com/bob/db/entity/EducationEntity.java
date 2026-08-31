package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "education", schema = "candidate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE candidate.education SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class EducationEntity extends BaseEntity<UUID> {

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "education_qualifications_id", nullable = false)
    private UUID educationQualificationsId;

    @Column(name = "institution_name", nullable = false, length = 255)
    private String institutionName;

    @Column(name = "specialization_id")
    private UUID specializationId;

    @Column(name = "education_type_id", nullable = false)
    private UUID educationTypeId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "university_name", length = 255)
    private String universityName;

    @Column(name = "other_qualification")
    private String otherQualification;

    @Column(name = "other_specialization")
    private String otherSpecialization;

    @Column(name = "is_submitted", nullable = false)
    private Boolean isSubmitted = false;
}
