package com.bob.db.entity;

import com.bob.db.enums.PreOnboardingStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pre_onboarding", schema = "candidate")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE candidate.pre_onboarding SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PreOnboardingEntity extends BaseEntity<UUID> {

    public static final String ENTITY_TYPE = "pre_onboarding";

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", length = 50)
    @Builder.Default
    private PreOnboardingStatus onboardingStatus=PreOnboardingStatus.NEW;

    @Column(name = "joining_location")
    private String joiningLocation;

    @Column(name = "expected_joining_date")
    private LocalDate expectedJoiningDate;

    @Column(name = "actual_joining_date")
    private LocalDate actualJoiningDate;

    @Column(name = "remarks", columnDefinition = "text")
    private String remarks;
}