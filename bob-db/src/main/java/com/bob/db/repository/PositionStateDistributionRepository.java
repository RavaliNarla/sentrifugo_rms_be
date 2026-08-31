package com.bob.db.repository;

import com.bob.db.entity.PositionStateDistributionEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PositionStateDistributionRepository extends JpaRepository<PositionStateDistributionEntity, UUID> {
    List<PositionStateDistributionEntity> findByJobPositionIdIn(List<UUID> positionIds);

    /** Find state+city specific entry (city_id NOT NULL). */
    @Query("SELECT p FROM PositionStateDistributionEntity p WHERE p.jobPosition.id = :positionId AND p.stateId = :stateId AND p.cityId = :cityId")
    Optional<PositionStateDistributionEntity> findByPositionAndStateAndCity(
            @Param("positionId") UUID positionId,
            @Param("stateId") UUID stateId,
            @Param("cityId") UUID cityId);

    /** Find state-only entry (city_id IS NULL). */
    @Query("SELECT p FROM PositionStateDistributionEntity p WHERE p.jobPosition.id = :positionId AND p.stateId = :stateId AND p.cityId IS NULL")
    Optional<PositionStateDistributionEntity> findByPositionAndStateWithoutCity(
            @Param("positionId") UUID positionId,
            @Param("stateId") UUID stateId);
}
