package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "language_master", schema = "common")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE common.language_master SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class LanguageMasterEntity extends BaseEntity<UUID> {

    @Column(name = "language_name")
    private String languageName;

    @Column(name = "state_id")
    private UUID stateId;

}
