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

@Entity
@Table(name = "locations", schema = "common")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE common.locations SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class LocationEntity extends BaseEntity<UUID> {

    /** IAM business-unit id — sync key (plant/office = BU). */
    @Column(name = "hrms_id", unique = true)
    private String hrmsId;

    @Column(name = "name", nullable = false)
    private String name;

    /** Code from HRMS emp_code_prefix (as-is); used in requisition numbers. */
    @Column(name = "code", length = 64, unique = true)
    private String code;

    /** Optional — HRMS BUs do not always map to RMS states. */
    @Column(name = "state_id")
    private UUID stateId;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;
}
