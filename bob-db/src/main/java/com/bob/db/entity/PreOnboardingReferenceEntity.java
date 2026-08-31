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

import java.util.UUID;

@Entity
@Table(name = "pre_onboarding_references", schema = "candidate")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE candidate.pre_onboarding_references SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PreOnboardingReferenceEntity extends BaseEntity<UUID> {

    public static final String ENTITY_TYPE = "pre_onboarding_references";

    @Column(name = "pre_onboarding_id", nullable = false)
    private UUID preOnboardingId;

    @Column(name = "reference_name", nullable = false, length = 255)
    private String referenceName;

    @Column(name = "designation", length = 255)
    private String designation;

    @Column(name = "organisation", length = 500)
    private String organisation;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @Column(name = "email_id", length = 255)
    private String emailId;
}