package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pre_onboarding_address", schema = "candidate")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE candidate.pre_onboarding_address SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PreOnboardingAddressEntity extends BaseEntity<UUID> {

    public static final String ENTITY_TYPE = "pre_onboarding_address";

    @Column(name = "pre_onboarding_id", nullable = false)
    private UUID preOnboardingId;

    @Column(name = "correspondence_address_line1", length = 500)
    private String correspondenceAddressLine1;

    @Column(name = "correspondence_address_line2", length = 500)
    private String correspondenceAddressLine2;

    @Column(name = "correspondence_address_line3", length = 500)
    private String correspondenceAddressLine3;

    @Column(name = "correspondence_city_id")
    private UUID correspondenceCityId;

    @Column(name = "correspondence_state_id")
    private UUID correspondenceStateId;

    @Column(name = "correspondence_district_id")
    private UUID correspondenceDistrictId;

    @Column(name = "correspondence_pincode", length = 10)
    private String correspondencePincode;

    @Column(name = "permanent_address_line1", length = 500)
    private String permanentAddressLine1;

    @Column(name = "permanent_address_line2", length = 500)
    private String permanentAddressLine2;

    @Column(name = "permanent_address_line3", length = 500)
    private String permanentAddressLine3;

    @Column(name = "permanent_city_id")
    private UUID permanentCityId;

    @Column(name = "permanent_state_id")
    private UUID permanentStateId;

    @Column(name = "permanent_district_id")
    private UUID permanentDistrictId;

    @Column(name = "permanent_pincode", length = 10)
    private String permanentPincode;

}