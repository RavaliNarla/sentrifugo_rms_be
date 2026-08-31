package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;
@Entity
@Table(name = "disability_categories", schema = "common")
@Data
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE common.disability_categories SET is_active = false WHERE id = ?")
@SQLRestriction("is_active = true")
public class DisabilityCategoriesEntity extends BaseEntity<UUID> {
    @Column(name = "disability_code", nullable = false)
    private String disabilityCode;

    @Column(name = "disability_name")
    private String disabilityName;

    @Column(name = "display_order")
    private Integer displayOrder;
}
