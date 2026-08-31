package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pre_onboarding_qualifications", schema = "candidate")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE candidate.pre_onboarding_qualifications SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PreOnboardingQualificationEntity extends BaseEntity<UUID> {

    public static final String ENTITY_TYPE = "pre_onboarding_qualifications";

    @Column(name = "pre_onboarding_id", nullable = false)
    private UUID preOnboardingId;

    @Column(name = "qualification_name")
    private UUID qualificationName;

    @Column(name = "institution_name", nullable = false, length = 500)
    private String institutionName;

    @Column(name = "specialization")
    private UUID specialization;

    @Column(name = "university_name", length = 500)
    private String universityName;

    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "passing_date")
    private LocalDate passingDate;

    @Column(name = "division", length = 100)
    private String division;

    @Column(name = "course_nature")
    private UUID courseNature;

    @Column(name = "education_id")
    private UUID educationId;
}