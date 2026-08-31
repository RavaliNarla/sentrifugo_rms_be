package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "qualification_group_mapping", schema = "common")
@SQLDelete(sql = "UPDATE common.qualification_group_mapping SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class QualificationGroupMappingEntity extends BaseEntity<UUID> {

    @Column(name = "education_qualification_id", nullable = false)
    private UUID educationQualificationId;

    @Column(name = "specialization_id")
    private UUID specializationId;

    @Column(name = "education_group_id", nullable = false)
    private UUID educationGroupId;
}