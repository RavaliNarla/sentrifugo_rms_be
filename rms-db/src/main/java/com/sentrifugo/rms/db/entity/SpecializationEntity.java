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
 * Specialization master (e.g. "Computer Science and Engineering", "Mechanical
 * Engineering" for a B.Tech education level). Optionally linked to an
 * EducationQualification; a null educationQualificationId means the
 * specialization applies generally / isn't tied to one specific education level.
 */
@Entity
@Table(name = "specializations", schema = "common")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE common.specializations SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class SpecializationEntity extends BaseEntity<UUID> {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "education_qualification_id")
    private UUID educationQualificationId;
}
