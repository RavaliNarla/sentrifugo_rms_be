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
@Table(name = "departments", schema = "common")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE common.departments SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class DepartmentEntity extends BaseEntity<UUID> {

    /** IAM department id — sync key. */
    @Column(name = "hrms_id", unique = true)
    private String hrmsId;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    /** Code from HRMS (as-is); used in requisition numbers. */
    @Column(name = "code", length = 64, unique = true)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Comma-separated HRMS business-unit ids linked to this department. */
    @Column(name = "hrms_business_unit_ids", columnDefinition = "TEXT")
    private String hrmsBusinessUnitIds;
}
