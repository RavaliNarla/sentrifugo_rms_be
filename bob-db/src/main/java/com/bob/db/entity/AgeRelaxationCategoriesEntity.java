package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "age_relaxation_categories", schema = "common")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE common.age_relaxation_categories SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class AgeRelaxationCategoriesEntity extends BaseEntity<UUID> {

    @Column(name = "age_relaxation_category_code", nullable = false, unique = true, length = 20)
    private String ageRelaxationCategoryCode;

    @Column(name = "age_relaxation_category_display_name", nullable = false, length = 150)
    private String ageRelaxationCategoryDisplayName;

    @Column(name = "relaxation_years")
    private Short relaxationYears;

    @Column(name = "description", columnDefinition = "text")
    private String description;

}
