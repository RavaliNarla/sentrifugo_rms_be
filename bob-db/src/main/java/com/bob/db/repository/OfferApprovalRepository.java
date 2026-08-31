package com.bob.db.repository;

import com.bob.db.entity.OfferApprovalsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
@Repository
public interface OfferApprovalRepository extends JpaRepository<OfferApprovalsEntity, UUID>, JpaSpecificationExecutor<OfferApprovalsEntity> {
    List<OfferApprovalsEntity> findByOfferIdIn(List<UUID> offerIds);
}
