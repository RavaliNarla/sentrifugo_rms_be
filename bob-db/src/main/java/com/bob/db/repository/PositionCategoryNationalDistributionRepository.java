package com.bob.db.repository;

import com.bob.db.entity.PositionCategoryNationalDistributionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PositionCategoryNationalDistributionRepository extends JpaRepository<PositionCategoryNationalDistributionEntity, UUID> {

    /** Find the main reservation-category row (not a disability row) for a position. */
    @Query("""
            SELECT n FROM PositionCategoryNationalDistributionEntity n
            WHERE n.jobPosition.id = :positionId
              AND n.reservationCategoryId = :reservationCategoryId
              AND n.isDisability = false
            """)
    Optional<PositionCategoryNationalDistributionEntity> findMainCategoryRow(
            @Param("positionId") UUID positionId,
            @Param("reservationCategoryId") UUID reservationCategoryId);

    /** Find the disability sub-row for a position. */
    @Query("""
            SELECT n FROM PositionCategoryNationalDistributionEntity n
            WHERE n.jobPosition.id = :positionId
              AND n.reservationCategoryId = :reservationCategoryId
              AND n.disabilityCategoryId = :disabilityCategoryId
              AND n.isDisability = true
            """)
    Optional<PositionCategoryNationalDistributionEntity> findDisabilityCategoryRow(
            @Param("positionId") UUID positionId,
            @Param("reservationCategoryId") UUID reservationCategoryId,
            @Param("disabilityCategoryId") UUID disabilityCategoryId);

    @Query("SELECT n FROM PositionCategoryNationalDistributionEntity n WHERE n.jobPosition.id = :positionId")
    List<PositionCategoryNationalDistributionEntity> findAllByPositionId(@Param("positionId") UUID positionId);
}
