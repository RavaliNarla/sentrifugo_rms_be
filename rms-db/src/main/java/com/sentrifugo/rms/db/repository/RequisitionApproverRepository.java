package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.RequisitionApproverEntity;
import com.sentrifugo.rms.db.enums.ApproverRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequisitionApproverRepository extends JpaRepository<RequisitionApproverEntity, UUID> {
    Optional<RequisitionApproverEntity> findByApproverId(UUID approverId);
    List<RequisitionApproverEntity> findByApproverRole(ApproverRole approverRole);
}
