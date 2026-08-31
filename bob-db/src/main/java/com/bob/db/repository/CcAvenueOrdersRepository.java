package com.bob.db.repository;

import com.bob.db.entity.CcAvenueOrdersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CcAvenueOrdersRepository extends JpaRepository<CcAvenueOrdersEntity, UUID> {
    Optional<CcAvenueOrdersEntity> findByTrackingId(String trackingId);
    Optional<CcAvenueOrdersEntity> findFirstByOrderIdOrderByCreatedDateDesc(UUID orderId);
}
