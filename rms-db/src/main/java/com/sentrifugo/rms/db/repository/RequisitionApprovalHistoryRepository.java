package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.RequisitionApprovalHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequisitionApprovalHistoryRepository extends JpaRepository<RequisitionApprovalHistoryEntity, UUID> {
    List<RequisitionApprovalHistoryEntity> findByRequisitionIdOrderByCreatedDateDesc(UUID requisitionId);
}
