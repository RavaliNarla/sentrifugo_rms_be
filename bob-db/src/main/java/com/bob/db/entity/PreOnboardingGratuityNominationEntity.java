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
@Table(name = "pre_onboarding_gratuity_nominations", schema = "candidate")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE candidate.pre_onboarding_gratuity_nominations SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PreOnboardingGratuityNominationEntity extends BaseEntity<UUID> {

    public static final String ENTITY_TYPE = "pre_onboarding_gratuity_nominations";

    @Column(name = "pre_onboarding_id", nullable = false)
    private UUID preOnboardingId;

    @Column(name = "nominee_name", nullable = false, length = 255)
    private String nomineeName;

    @Column(name = "nominee_address", columnDefinition = "text")
    private String nomineeAddress;

    @Column(name = "nominee_relationship", length = 100)
    private String nomineeRelationship;

    @Column(name = "nominee_age")
    private Integer nomineeAge;

    @Column(name = "share_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal sharePercentage;
}