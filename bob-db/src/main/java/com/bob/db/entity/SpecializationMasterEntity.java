package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Data
@Entity
@Table(name = "specialization_master", schema = "common")
@SQLDelete(sql = "UPDATE common.specialization_master SET is_active = false WHERE id = ?")
@SQLRestriction("is_active = true")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SpecializationMasterEntity extends BaseEntity<UUID> {

    @Column(name = "specialization_name", nullable = false)
    private String specializationName;

    @Column(name = "education_qualifications_id", nullable = false)
    private UUID educationQualificationsId;

    @Column(name = "specialization_code",nullable = false)
    private String specializationCode;
}
