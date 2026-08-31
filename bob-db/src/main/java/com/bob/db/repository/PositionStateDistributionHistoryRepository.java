package com.bob.db.repository;

import com.bob.db.entity.PositionStateDistributionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PositionStateDistributionHistoryRepository extends JpaRepository<PositionStateDistributionHistoryEntity, UUID> {
}
