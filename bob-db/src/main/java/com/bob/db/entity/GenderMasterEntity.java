package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "gender_master", schema = "common")
@Data
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE common.gender_master SET is_active = false WHERE id = ?")
@SQLRestriction("is_active = true")
public class GenderMasterEntity extends BaseEntity<UUID> {

    @Column(name = "gender", nullable = false, unique = true, length = 20)
    private String gender;

}
