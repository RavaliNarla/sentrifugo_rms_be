package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.JobPositionEntity;
import com.sentrifugo.rms.db.enums.PositionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobPositionRepository extends JpaRepository<JobPositionEntity, UUID> {
    List<JobPositionEntity> findByRequisitionId(UUID requisitionId);
    List<JobPositionEntity> findByRequisitionIdIn(List<UUID> requisitionIds);
    List<JobPositionEntity> findByRequisitionIdAndStatus(UUID requisitionId, PositionStatus status);

    boolean existsByRequisitionIdAndDepartmentIdAndLocationIdAndPositionTitleId(
            UUID requisitionId, UUID departmentId, UUID locationId, UUID positionTitleId);

    // Distinct values actually used across all positions - drive the Job Postings filter dropdowns.
    @Query("SELECT DISTINCT pt.name FROM JobPositionEntity p JOIN PositionTitleEntity pt ON pt.id = p.positionTitleId ORDER BY pt.name")
    List<String> findDistinctPositionTitleNames();

    @Query("SELECT DISTINCT d.name FROM JobPositionEntity p JOIN DepartmentEntity d ON d.id = p.departmentId ORDER BY d.name")
    List<String> findDistinctDepartmentNames();

    @Query("SELECT DISTINCT l.name FROM JobPositionEntity p JOIN LocationEntity l ON l.id = p.locationId ORDER BY l.name")
    List<String> findDistinctLocationNames();
}
