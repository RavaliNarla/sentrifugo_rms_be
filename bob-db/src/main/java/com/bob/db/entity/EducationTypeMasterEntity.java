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
@Table(name = "education_type_master", schema = "common")
@SQLDelete(sql = "UPDATE common.education_type_master SET is_active = false WHERE id = ?")
@SQLRestriction("is_active = true")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EducationTypeMasterEntity extends BaseEntity<UUID> {

    @Column(name = "education_type", nullable = false)
    private String educationType;

}
