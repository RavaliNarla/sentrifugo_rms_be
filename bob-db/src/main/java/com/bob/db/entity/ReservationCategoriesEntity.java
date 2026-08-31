package com.bob.db.entity;

import com.bob.db.enums.ReservationType;
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
@Table(name = "reservation_categories",schema = "common")
@Data
@SQLDelete(sql = "UPDATE common.reservation_categories SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class ReservationCategoriesEntity  extends BaseEntity<UUID>{

    @Column(name = "category_code", nullable = false)
    private String categoryCode;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "category_desc", columnDefinition = "text")
    private String categoryDesc;

    @Column(name = "display_order")
    private Integer displayOrder;


    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_type")
    private ReservationType reservationType = ReservationType.VERTICAL;

}
