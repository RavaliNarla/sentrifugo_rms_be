package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pre_onboarding_personal_details", schema = "candidate")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE candidate.pre_onboarding_personal_details SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PreOnboardingPersonalDetailsEntity extends BaseEntity<UUID> {

    public static final String ENTITY_TYPE = "pre_onboarding_personal_details";

    @Column(name = "pre_onboarding_id", nullable = false)
    private UUID preOnboardingId;

    @Column(name = "gender_id")
    private UUID genderId;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "marital_status_id")
    private UUID maritalStatusId;

    @Column(name = "cibil_score", precision = 5, scale = 2)
    private BigDecimal cibilScore;

    @Column(name = "cibil_score_date")
    private LocalDate cibilScoreDate;

    @Column(name = "is_person_with_disability")
    @Builder.Default
    private Boolean isPersonWithDisability = false;

    @Column(name = "disability_certificate_board_name", length = 255)
    private String disabilityCertificateBoardName;

    @Column(name = "disability_certificate_reference_no", length = 100)
    private String disabilityCertificateReferenceNo;

    @Column(name = "disability_certificate_issue_date")
    private LocalDate disabilityCertificateIssueDate;

}