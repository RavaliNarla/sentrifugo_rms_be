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
@Table(name = "university_master", schema = "common")
@SQLDelete(sql = "UPDATE common.university_master SET is_active = false WHERE id = ?")
@SQLRestriction("is_active = true")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UniversityMasterEntity extends BaseEntity<UUID> {

    @Column(name = "university_name", nullable = false)
    private String universityName;

}
