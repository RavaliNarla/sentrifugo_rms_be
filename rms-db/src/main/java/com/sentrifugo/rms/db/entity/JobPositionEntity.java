package com.sentrifugo.rms.db.entity;

import com.sentrifugo.rms.db.enums.EmploymentType;
import com.sentrifugo.rms.db.enums.PositionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "job_positions", schema = "recruitment")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE recruitment.job_positions SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class JobPositionEntity extends BaseEntity<UUID> {

    @Column(name = "requisition_id", nullable = false)
    private UUID requisitionId;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "position_title_id", nullable = false)
    private UUID positionTitleId;

    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;

    @Column(name = "education_qualification_id")
    private UUID educationQualificationId;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type")
    @Builder.Default
    private EmploymentType employmentType = EmploymentType.REGULAR;

    @Column(name = "vacancies")
    @Builder.Default
    private Integer vacancies = 1;

    @Column(name = "approval_doc_url")
    private String approvalDocUrl;

    @Column(name = "approved_by_id")
    private UUID approvedById;

    @Column(name = "approved_on")
    private LocalDate approvedOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PositionStatus status = PositionStatus.NEW;
}
