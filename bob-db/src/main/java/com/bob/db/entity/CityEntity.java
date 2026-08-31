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
@Table(name = "city",schema = "common")
@Data
@SQLDelete(sql = "UPDATE common.city SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CityEntity  extends BaseEntity<UUID>{

    @Column(name="city_name")
    private String cityName;

    @Column(name="state_id")
    private UUID stateId;

    @Column(name="district_id")
    private UUID districtId;

//    @Column(name="created_date", columnDefinition = "timestamp default now()")
//    @CreationTimestamp
//    private LocalDateTime createdDate;

}
