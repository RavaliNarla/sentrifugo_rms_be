package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "master_positions", schema = "recruitment", uniqueConstraints = {
        @UniqueConstraint(columnNames = "position_code")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recruitment.master_positions SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class MasterPositionsEntity extends BaseEntity<UUID> {

    @Column(name = "position_code", length = 20, unique = true, insertable = false)
    private String positionCode;

    @Column(name = "position_name", length = 255)
    private String positionName;

    @Column(name = "position_description", columnDefinition = "text")
    private String positionDescription;

    @Column(name = "dept_id")
    private UUID deptId;

    @Column(name = "total_vacancies")
    private Integer totalVacancies;

    @Column(name = "eligibility_age_min")
    private Integer eligibilityAgeMin;

    @Column(name = "eligibility_age_max")
    private Integer eligibilityAgeMax;

    @Column(name = "employment_type")
    private UUID employmentType;

    @Column(name = "cibil_score", precision = 38, scale = 2)
    private BigDecimal cibilScore;

    @Column(name = "grade_id", nullable = false)
    private UUID gradeId;

    @Column(name = "mandatory_education", columnDefinition = "text")
    private String mandatoryEducation;

    @Column(name = "preferred_education", columnDefinition = "text")
    private String preferredEducation;

    @Column(name = "mandatory_experience", nullable = false, columnDefinition = "text")
    private String mandatoryExperience;

    @Column(name = "preferred_experience", nullable = false, columnDefinition = "text")
    private String preferredExperience;

    @Column(name = "roles_responsibilities", nullable = false, columnDefinition = "text")
    private String rolesResponsibilities;

}
