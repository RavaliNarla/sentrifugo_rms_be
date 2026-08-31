//package com.bob.db.entity;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.hibernate.annotations.SQLDelete;
//import org.hibernate.annotations.Where;
//
//import java.util.UUID;
//import java.util.UUID;
//
//@Entity
//@Table(name = "job_posting_location", schema = "recruitment")
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//@SQLDelete(sql = "UPDATE recruitment.job_posting_location SET is_active = false WHERE id = ?")
//@Where(clause = "is_active = true")
//public class JobPostingLocationEntity  extends BaseEntity<UUID>{
//
//    @Column(name = "position_id", nullable = false)
//    private UUID positionId;
//    @Column(name = "dept_id", nullable = false)
//    private UUID deptId;
//
//    @Column(name = "location_id", nullable = false)
//    private UUID locationId;
//
//    @Column(name = "country_id")
//    private UUID countryId;
//
//    @Column(name = "state_id")
//    private UUID stateId;
//
//    @Column(name = "city_id")
//    private UUID cityId;
//
//
//}
