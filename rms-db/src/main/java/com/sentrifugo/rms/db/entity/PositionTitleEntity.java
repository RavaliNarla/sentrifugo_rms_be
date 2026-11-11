package com.sentrifugo.rms.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

/**
 * Position / designation master. HRMS designations sync here without a department link;
 * {@code departmentId}, {@code jobDescription}, and {@code minimumExperienceYears} are RMS-only.
 */
@Entity
@Table(name = "position_titles", schema = "common")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE common.position_titles SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PositionTitleEntity extends BaseEntity<UUID> {

    /** IAM designation id — sync key. */
    @Column(name = "hrms_id", unique = true)
    private String hrmsId;

    @Column(name = "name", nullable = false)
    private String name;

    /** RMS-only optional link; null = appears under every department on Add Position. */
    @Column(name = "department_id")
    private UUID departmentId;

    /** RMS-only Roles & Responsibilities default. */
    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;

    /** RMS-only minimum experience default (null = None). */
    @Column(name = "minimum_experience_years")
    private Integer minimumExperienceYears;
}
