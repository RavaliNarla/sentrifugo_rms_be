package com.bob.db.repository;

import com.bob.db.entity.OfferApprovalHistoryEntity;

import java.util.List;

public interface OfferApprovalHistoryCustom {
    OfferApprovalHistoryEntity saveWithWorkflow(OfferApprovalHistoryEntity newEntity,String comments);
    List<OfferApprovalHistoryEntity> saveAllWithWorkflow(List<OfferApprovalHistoryEntity> newEntities,String comments);
}