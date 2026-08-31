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

import java.util.UUID;

@Entity
@Table(name = "employement_types", schema = "common")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE common.employement_types SET is_active = false WHERE id = ?")
@SQLRestriction("is_active = true")
public class EmployementTypesEntity extends BaseEntity<UUID> {

    @Column(name = "type_code", length = 10)
    private String typeCode;

    @Column(name = "type_name", length = 50)
    private String typeName;
}
