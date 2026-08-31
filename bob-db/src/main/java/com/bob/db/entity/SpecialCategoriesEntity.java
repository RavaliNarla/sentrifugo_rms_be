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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "special_categories",schema = "common")
@Data
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE common.special_categories SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class SpecialCategoriesEntity  extends BaseEntity<UUID>{

    @Column(name = "special_category_code",nullable = false)
    private String specialCategoryCode;

    @Column(name = "special_category_name")
    private String specialCategoryName;

    @Column(name = "special_category_desc", columnDefinition = "text")
    private String specialCategoryDesc;


}
