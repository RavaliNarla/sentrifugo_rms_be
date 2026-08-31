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
@Table(name = "district", schema = "common")
@Data
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE common.district SET is_active = false WHERE id = ?")
@SQLRestriction("is_active = true")
public class DistrictEntity extends BaseEntity<UUID> {
    @Column(name = "district_name", nullable = false, length = 255)
    private String districtName;

    @Column(name = "state_id")
    private UUID stateId;
}
