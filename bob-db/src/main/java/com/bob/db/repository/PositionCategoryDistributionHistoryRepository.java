package com.bob.db.repository;

import com.bob.db.entity.PositionCategoryDistributionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PositionCategoryDistributionHistoryRepository extends JpaRepository<PositionCategoryDistributionHistoryEntity, UUID> {
}
