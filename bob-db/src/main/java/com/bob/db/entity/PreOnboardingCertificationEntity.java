package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pre_onboarding_certifications", schema = "candidate")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE candidate.pre_onboarding_certifications SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PreOnboardingCertificationEntity extends BaseEntity<UUID> {

    public static final String ENTITY_TYPE = "pre_onboarding_certifications";

    @Column(name = "pre_onboarding_id", nullable = false)
    private UUID preOnboardingId;

    @Column(name = "certification_name", nullable = false, length = 500)
    private String certificationName;

    @Column(name = "institution_name", length = 500)
    private String institutionName;

    @Column(name = "passing_date")
    private LocalDate passingDate;

    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "candidate_certificate_id")
    private UUID candidateCertificateId;
}