package com.bob.db.repository;

import com.bob.db.entity.RequisitionApproversEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequisitionApproversRepository extends JpaRepository<RequisitionApproversEntity, UUID> {

    Optional<RequisitionApproversEntity> findByApproverId(UUID approverId);

    Optional<RequisitionApproversEntity> findByApproverRole(RequisitionApproversEntity.ApproverRole approverRole);
}
