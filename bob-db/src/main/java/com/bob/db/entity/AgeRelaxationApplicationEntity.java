package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Table(name = "age_relaxation_application", schema = "candidate")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE candidate.age_relaxation_application SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class AgeRelaxationApplicationEntity extends BaseEntity<UUID> {

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "position_id", nullable = false)
    private UUID positionId;

    @Column(name = "application_id")
    private UUID applicationId;

    @Column(name = "relaxation_applied", columnDefinition = "boolean DEFAULT false")
    private Boolean relaxationApplied;

    @Column(name = "applied_rel_reservation_category_id")
    private UUID appliedRelReservationCategoryId;

    @Column(name = "applied_rel_special_category_id")
    private UUID appliedRelSpecialCategoryId;

    @Column(name = "actual_applied_relaxation_years")
    private Integer actualAppliedRelaxationYears;

    @Column(name = "age_years_at_ref_date")
    private Integer ageYearsAtRefDate;

    @Column(name = "state_id")
    private UUID stateId;

    @Column(name = "state_age_validation_passed")
    private Boolean stateAgeValidationPassed;

    @Column(name = "state_vacancy_validation_passed")
    private Boolean stateVacancyValidationPassed;

    @Column(name = "city_id")
    private UUID cityId;

}
