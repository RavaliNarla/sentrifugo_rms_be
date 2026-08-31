package com.bob.db.entity;

import java.sql.Timestamp;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "exserviceman_categories", schema = "common")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE common.exserviceman_categories SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class ExservicemanCategoryEntity extends BaseEntity<UUID> {

    @Column(name = "exs_category_code", unique = true, nullable = false, length = 30)
    private String exsCategoryCode;

    @Column(name = "exs_category_name", length = 100)
    private String exsCategoryName;

    @Column(name = "display_order")
    private Integer displayOrder;
}
