package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pre_onboarding_domicile_details", schema = "candidate")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE candidate.pre_onboarding_domicile_details SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PreOnboardingDomicileDetailsEntity extends BaseEntity<UUID> {

    public static final String ENTITY_TYPE = "pre_onboarding_domicile_details";

    @Column(name = "pre_onboarding_id", nullable = false)
    private UUID preOnboardingId;

    @Column(name = "state_of_origin_id")
    private UUID stateOfOriginId;

    @Column(name = "religion_id")
    private UUID religionId;

    @Column(name = "is_minority")
    @Builder.Default
    private Boolean isMinority = false;

    @Column(name = "city_id")
    private UUID cityId;

    @Column(name = "state_id")
    private UUID stateId;

    @Column(name = "district_id")
    private UUID districtId;

    @Column(name = "caste_category_id")
    private UUID casteCategoryId;

    @Column(name = "caste_community")
    private String casteCommunity;

    @Column(name = "nationality", length = 100)
    private String nationality;

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Column(name = "pan_no", length = 20)
    private String panNo;

    @Column(name = "pan_applied")
    @Builder.Default
    private Boolean panApplied = false;

    @Column(name = "pan_reference_no", length = 100)
    private String panReferenceNo;

    @Column(name = "mobile_no", length = 20)
    private String mobileNo;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "voter_id_card_no", length = 50)
    private String voterIdCardNo;

    @Column(name = "driving_license_no", length = 50)
    private String drivingLicenseNo;

    @Column(name = "passport_no", length = 50)
    private String passportNo;

}