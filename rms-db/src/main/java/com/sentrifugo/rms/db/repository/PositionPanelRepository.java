package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.PositionPanelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PositionPanelRepository extends JpaRepository<PositionPanelEntity, UUID> {
    List<PositionPanelEntity> findByPositionId(UUID positionId);
    List<PositionPanelEntity> findByPositionIdAndEndDateGreaterThanEqual(UUID positionId, LocalDate today);
    boolean existsByPanelId(UUID panelId);
}
