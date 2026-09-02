package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.JobPositionEntity;
import com.sentrifugo.rms.db.enums.PositionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobPositionRepository extends JpaRepository<JobPositionEntity, UUID> {
    List<JobPositionEntity> findByRequisitionId(UUID requisitionId);
    List<JobPositionEntity> findByRequisitionIdIn(List<UUID> requisitionIds);
    List<JobPositionEntity> findByRequisitionIdAndStatus(UUID requisitionId, PositionStatus status);
}
