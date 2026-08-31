package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "skill",schema = "common")
@Data
@SQLDelete(sql = "UPDATE common.skill SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class SkillEntity  extends BaseEntity<UUID>{

    @Column(name = "skill_name")
    private String skillName;

    @Column(name = "skill_desc", columnDefinition = "text")
    private String skillDesc;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

}
