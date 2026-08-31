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
@Table(name = "pre_onboarding_previous_organisations", schema = "candidate")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
//@SQLDelete(sql = "UPDATE candidate.pre_onboarding_previous_organisations SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PreOnboardingPreviousOrganisationEntity extends BaseEntity<UUID> {

    public static final String ENTITY_TYPE = "pre_onboarding_previous_organisations";

    @Column(name = "pre_onboarding_id", nullable = false)
    private UUID preOnboardingId;

    @Column(name = "pran_card_number", length = 20)
    private String pranCardNumber;

    @Column(name = "employee_code", length = 100)
    private String employeeCode;

    @Column(name = "organisation_name", nullable = false, length = 500)
    private String organisationName;

    @Column(name = "organisation_address", columnDefinition = "text")
    private String organisationAddress;

    @Column(name = "designation", length = 255)
    private String designation;

    @Column(name = "work_description", columnDefinition = "text")
    private String workDescription;

    @Column(name = "last_drawn_salary", precision = 12, scale = 2)
    private BigDecimal lastDrawnSalary;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    @Column(name = "reason_for_leaving", columnDefinition = "text")
    private String reasonForLeaving;

    @Column(name = "employer_email", length = 255)
    private String employerEmail;

    @Column(name = "employer_contact_no", length = 20)
    private String employerContactNo;

    @Column(name = "is_currently_working")
    @Builder.Default
    private Boolean isCurrentlyWorking = false;
}