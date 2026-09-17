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
 * Certification master (e.g. "NCCBM Certified Quality Controller") - optional,
 * single-select field on a Position, replacing the old free-text Certifications input.
 */
@Entity
@Table(name = "certifications", schema = "common")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE common.certifications SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CertificationEntity extends BaseEntity<UUID> {

    @Column(name = "name", nullable = false, unique = true)
    private String name;
}
