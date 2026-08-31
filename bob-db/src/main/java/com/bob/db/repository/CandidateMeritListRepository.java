package com.bob.db.repository;

import com.bob.db.entity.CandidateMeritListEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateMeritListRepository extends JpaRepository<CandidateMeritListEntity, UUID> {
    List<CandidateMeritListEntity> findByPositionId(UUID positionId);
    List<CandidateMeritListEntity> findByPositionIdAndSelectedTrue(UUID positionId);
    List<CandidateMeritListEntity> findByApplicationIdIn(List<UUID> applicationIds);
    Optional<CandidateMeritListEntity> findByApplicationId(UUID applicationId);
    List<CandidateMeritListEntity> findByPositionIdIn(Collection<UUID> positionIds);
}
