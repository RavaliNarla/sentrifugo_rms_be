package com.sentrifugo.rms.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

/**
 * Position Master (Section 5 of the requirements doc): pre-defines positions per
 * department so the Add Position screen can auto-populate the Positions dropdown
 * from the selected Department, and prefill Job Description / Minimum Experience.
 */
@Entity
@Table(name = "position_titles", schema = "common",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "department_id"}))
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE common.position_titles SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PositionTitleEntity extends BaseEntity<UUID> {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;

    @Column(name = "minimum_experience_years")
    private Integer minimumExperienceYears;
}
