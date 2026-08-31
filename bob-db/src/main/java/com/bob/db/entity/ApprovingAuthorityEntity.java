package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "approving_authority", schema = "common")
@NoArgsConstructor
@AllArgsConstructor
@Data
@SuperBuilder
@SQLDelete(sql = "UPDATE common.approving_authority SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class ApprovingAuthorityEntity extends BaseEntity<UUID> {

    @Column(name = "authority_name", nullable = false, unique = true, length = 100)
    private String authorityName;

}
