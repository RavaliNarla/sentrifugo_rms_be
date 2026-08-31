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
import java.util.UUID;

@Entity
@Table(name = "country",schema = "common")
@Data
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE common.country SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CountryEntity  extends BaseEntity<UUID>{

    @Column(name = "country_name")
    private String countryName;



}
