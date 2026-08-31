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
//@Table(name = "job_vacancies", schema = "recruitment")
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//@SQLDelete(sql = "UPDATE recruitment.job_vacancies SET is_active = false WHERE id = ?")
//@Where(clause = "is_active = true")
//public class JobVacanciesEntity  extends BaseEntity<UUID>{
//
//    @Column(name = "position_id", nullable = false)
//    private UUID positionId;
//
//    @Column(name = "special_cat_id", nullable = false)
//    private UUID specialCatId;
//
//    @Column(name = "reservation_cat_id", nullable = false)
//    private UUID reservationCatId;
//
//    @Column(name = "location_id", nullable = false)
//    private UUID locationId;
//
//    @Column(name = "no_of_vacancies", nullable = false)
//    private Integer noOfVacancies;
//}
