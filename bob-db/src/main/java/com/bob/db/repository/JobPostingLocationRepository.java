//package com.bob.db.repository;
//
//import com.bob.db.entity.JobPostingLocationEntity;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.UUID;
//
//@Repository
//public interface JobPostingLocationRepository extends JpaRepository<JobPostingLocationEntity, UUID> {
//
//    JobPostingLocationEntity findByPositionId(UUID positionId);
//
//    List<JobPostingLocationEntity> findByPositionIdIn(List<UUID> positionIds);
//
//}
