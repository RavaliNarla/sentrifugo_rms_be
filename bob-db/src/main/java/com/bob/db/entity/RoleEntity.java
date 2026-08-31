package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;
@Entity
@Table(name = "roles", schema = "hr")
@Data
@SQLDelete(sql = "UPDATE hr.roles SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class RoleEntity extends BaseEntity<UUID>{
    @Column(name = "role_code", nullable = false, unique = true, length = 50)
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Column(name = "role_description")
    private String roleDescription;

    @Column(name = "role_category")
    private String roleCategory;

}
