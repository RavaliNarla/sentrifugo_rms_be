package com.bob.db.repository;

import com.bob.db.entity.JobRequisitionEditRequestEntity;
import com.bob.db.enums.RequisitionEditStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRequisitionEditRequestRepository extends JpaRepository<JobRequisitionEditRequestEntity, UUID> {
    Optional<JobRequisitionEditRequestEntity> findTopByParentRequisitionIdAndIsActiveTrueAndRequisitionStatusInOrderByCreatedDateDesc(
            UUID parentRequisitionId,
            Collection<RequisitionEditStatus> statuses
    );

    List<JobRequisitionEditRequestEntity> findAllByParentRequisitionIdAndRequisitionStatus(
            UUID parentRequisitionId,
            RequisitionEditStatus status
    );
}
