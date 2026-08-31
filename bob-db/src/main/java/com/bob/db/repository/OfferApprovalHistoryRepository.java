package com.bob.db.repository;

import com.bob.db.entity.OfferApprovalHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OfferApprovalHistoryRepository extends JpaRepository<OfferApprovalHistoryEntity, UUID>, JpaSpecificationExecutor<OfferApprovalHistoryEntity>,OfferApprovalHistoryCustom {
}
