package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "education_groups", schema = "common")
@SQLDelete(sql = "UPDATE common.education_groups SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class EducationGroupsEntity extends BaseEntity<UUID> {

    @Column(name = "group_code", nullable = false, length = 20)
    private String groupCode;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(name = "display_order")
    private Integer displayOrder;
}