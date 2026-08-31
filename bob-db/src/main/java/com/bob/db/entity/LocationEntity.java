//package com.bob.db.entity;
//
//import jakarta.persistence.*;
//import lombok.Data;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.SQLDelete;
//import org.hibernate.annotations.UpdateTimestamp;
//import org.hibernate.annotations.Where;
//import org.springframework.data.annotation.CreatedBy;
//import org.springframework.data.annotation.LastModifiedBy;
//import org.springframework.data.jpa.domain.support.AuditingEntityListener;
//
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//@Entity
//@Table(name = "locations",schema = "common")
//@Data
//@SQLDelete(sql = "UPDATE common.locations SET is_active = false WHERE id = ?")
//@Where(clause = "is_active = true")
//public class LocationEntity  extends BaseEntity<UUID>{
//
//    @Column(name = "location_name")
//    private String locationName;
//
//    @Column(name = "city_id")
//    private UUID cityId;
//
//}
