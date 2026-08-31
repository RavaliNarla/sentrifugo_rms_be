package com.bob.db.repository;

import com.bob.db.entity.PositionCategoryNationalDistributionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PositionCategoryNationalDistributionHistoryRepository extends JpaRepository<PositionCategoryNationalDistributionHistoryEntity, UUID> {
}
