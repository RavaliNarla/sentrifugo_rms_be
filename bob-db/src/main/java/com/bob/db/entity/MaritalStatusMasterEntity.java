package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "marital_status_master",schema = "common")
@Data
@SQLDelete(sql = "UPDATE common.marital_status_master SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class MaritalStatusMasterEntity extends BaseEntity<UUID> {

    @Column(name = "marital_status", length = 100, nullable = false)
    private String maritalStatus;
}
