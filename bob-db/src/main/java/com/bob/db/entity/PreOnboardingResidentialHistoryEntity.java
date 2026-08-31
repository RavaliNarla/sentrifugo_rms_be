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

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pre_onboarding_residential_history", schema = "candidate")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
//@SQLDelete(sql = "UPDATE candidate.pre_onboarding_residential_history SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PreOnboardingResidentialHistoryEntity extends BaseEntity<UUID> {

    public static final String ENTITY_TYPE = "pre_onboarding_residential_history";

    @Column(name = "pre_onboarding_id", nullable = false)
    private UUID preOnboardingId;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(name = "address_line1", nullable = false, length = 500)
    private String addressLine1;

    @Column(name = "address_line2", length = 500)
    private String addressLine2;

    @Column(name = "address_line3", length = 500)
    private String addressLine3;

    @Column(name = "city_id")
    private UUID cityId;

    @Column(name = "district_id")
    private UUID districtId;

    @Column(name = "state_id")
    private UUID stateId;

    @Column(name = "pincode", length = 10)
    private String pincode;
}