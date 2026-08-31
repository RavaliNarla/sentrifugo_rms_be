package com.bob.db.repository;

import com.bob.db.entity.JobRequisitionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface JobRequisitionHistoryRepository extends JpaRepository<JobRequisitionHistoryEntity, UUID> {

    @Query("SELECT COALESCE(MAX(h.versionNo), 0) FROM JobRequisitionHistoryEntity h WHERE h.requisitionId = :requisitionId")
    Integer findLatestVersionNo(@Param("requisitionId") UUID requisitionId);
}
