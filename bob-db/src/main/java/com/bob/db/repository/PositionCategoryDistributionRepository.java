package com.bob.db.repository;

import com.bob.db.entity.PositionCategoryDistributionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PositionCategoryDistributionRepository extends JpaRepository<PositionCategoryDistributionEntity, UUID> {

    @Query("""
            SELECT DISTINCT c FROM PositionCategoryDistributionEntity c
            JOIN FETCH c.positionStateDistribution psd
            WHERE psd.id IN :stateIds
            """)
    List<PositionCategoryDistributionEntity> findAllByStateDistributionIdInWithFetch(@Param("stateIds") Collection<UUID> stateIds);

    /** Find the main reservation-category row (not a disability row) for a state distribution. */
    @Query("""
            SELECT c FROM PositionCategoryDistributionEntity c
            WHERE c.positionStateDistribution.id = :stateDistId
              AND c.reservationCategoryId = :reservationCategoryId
              AND c.isDisability = false
            """)
    Optional<PositionCategoryDistributionEntity> findMainCategoryRow(
            @Param("stateDistId") UUID stateDistId,
            @Param("reservationCategoryId") UUID reservationCategoryId);

    /** Find the disability sub-row for a state distribution. */
    @Query("""
            SELECT c FROM PositionCategoryDistributionEntity c
            WHERE c.positionStateDistribution.id = :stateDistId
              AND c.reservationCategoryId = :reservationCategoryId
              AND c.disabilityCategoryId = :disabilityCategoryId
              AND c.isDisability = true
            """)
    Optional<PositionCategoryDistributionEntity> findDisabilityCategoryRow(
            @Param("stateDistId") UUID stateDistId,
            @Param("reservationCategoryId") UUID reservationCategoryId,
            @Param("disabilityCategoryId") UUID disabilityCategoryId);
}
