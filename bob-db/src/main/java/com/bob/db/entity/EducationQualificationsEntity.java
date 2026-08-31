package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "education_qualifications",schema = "common")
@SQLDelete(sql = "UPDATE common.education_qualifications SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class EducationQualificationsEntity extends BaseEntity<UUID> {

    @Column(name = "level_id")
    private UUID levelId;

    @Column(name = "qualification_code", length = 100, nullable = false)
    private String qualificationCode;

    @Column(name = "qualification_name", length = 255, nullable = false)
    private String qualificationName;

    @Column(name = "display_order")
    private Integer displayOrder;

}

