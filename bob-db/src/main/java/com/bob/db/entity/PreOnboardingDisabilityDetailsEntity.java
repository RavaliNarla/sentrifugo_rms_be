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
import java.util.UUID;

@Entity
@Table(name = "pre_onboarding_disability_details", schema = "candidate")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "is_active = true")
public class PreOnboardingDisabilityDetailsEntity extends BaseEntity<UUID> {

    public static final String ENTITY_TYPE = "pre_onboarding_disability_details";

    @Column(name = "pre_onboarding_id", nullable = false)
    private UUID preOnboardingId;

    @Column(name = "disability_id")
    private UUID disabilityId;

    @Column(name = "disability_type", length = 255)
    private String disabilityType;

    @Column(name = "disability_percentage", precision = 5, scale = 2)
    private BigDecimal disabilityPercentage;
}