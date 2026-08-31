//package com.bob.db.repository;
//
//import com.bob.db.entity.JobVacanciesEntity;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.UUID;
//
//public interface JobVacanciesRepository extends JpaRepository<JobVacanciesEntity,UUID> {
//
//    JobVacanciesEntity findByPositionId(UUID positionId);
//
//    List<JobVacanciesEntity> findByPositionIdIn(List<UUID> posIds);
//
//    // Update by position id
//}
