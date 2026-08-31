package com.bob.db.repository;

import com.bob.db.entity.JobPositionsEntity;
import com.bob.db.enums.PositionStatus;
import com.bob.db.enums.RequisitionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface PositionsRepository extends JpaRepository<JobPositionsEntity, UUID>, JpaSpecificationExecutor<JobPositionsEntity> {
//    List<JobPositionsEntity> findByPositionIdIn(List<UUID> positionIds);

    List<JobPositionsEntity> findByRequisitionIdIn(List<UUID> reqIds);

    List<JobPositionsEntity> findAllByRequisitionId(UUID requisitionId);

    // Pessimistic write lock for publish path; locks all positions belonging to a requisition.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT jp FROM JobPositionsEntity jp WHERE jp.requisitionId = :requisitionId")
    List<JobPositionsEntity> findAllByRequisitionIdForUpdate(@Param("requisitionId") UUID requisitionId);

    List<JobPositionsEntity> findAllByIsActiveTrue();

    List<JobPositionsEntity> findAllByIdIn(List<UUID> positionIds);

    /**
     * Loads positions with state distribution rows only. Nested category rows must be loaded separately
     * (see {@link PositionCategoryDistributionRepository#findAllByStateDistributionIdInWithFetch}) to avoid
     * {@link org.hibernate.loader.MultipleBagFetchException}.
     */
    @Query("""
            SELECT DISTINCT jp FROM JobPositionsEntity jp
            LEFT JOIN FETCH jp.positionStateDistributions
            WHERE jp.id IN :ids
            """)
    List<JobPositionsEntity> findAllByIdInWithStateDistributionsOnly(@Param("ids") Collection<UUID> ids);

    /**
     * Initializes national distribution bags for the same positions (run after
     * state + category loading in the same persistence context).
     */
    @Query("""
            SELECT DISTINCT jp FROM JobPositionsEntity jp
            LEFT JOIN FETCH jp.positionCategoryNationalDistributions
            WHERE jp.id IN :ids
            """)
    List<JobPositionsEntity> findAllByIdInWithNationalDistributions(@Param("ids") Collection<UUID> ids);

//    List<JobPositionsEntity> findAllByPositionIdInAndIsActiveTrue(Collection<UUID> positionIds);

    List<JobPositionsEntity> findAllByRequisitionIdInAndPositionStatus(Collection<UUID> requisitionIds, PositionStatus positionStatus);
    List<JobPositionsEntity> findAllByRequisitionIdIn(Collection<UUID> requisitionIds);

//    List<JobPositionsEntity> findByRequisitionId(UUID requisitionId, Boolean isActive);

//    List<JobPositionsEntity> findByPositionIdInAndIsActive(Collection<UUID> positionIds, Boolean isActive);

//    List<JobPositionsEntity> findByJobRelaxationPolicyId(UUID jobRelaxationPolicyId); // Commented out - field doesn't exist in entity

    List<JobPositionsEntity> findTop5ByOrderByCreatedDateDesc();

    List<JobPositionsEntity> findByRequisitionIdInAndPositionStatus(Set<UUID> requisitionIds, String jobPositionStatusActive);

    @Query("""
    SELECT DISTINCT jp FROM JobPositionsEntity jp
    JOIN JobRequisitionsEntity jr ON jr.id = jp.requisitionId
    LEFT JOIN MasterPositionsEntity mp ON mp.id = jp.masterPositionId
    LEFT JOIN PositionStateDistributionEntity psd ON psd.jobPosition.id = jp.id
    WHERE jp.positionStatus = :positionStatus
    AND jr.requisitionStatus = :requisitionStatus
    
    AND (:deptIds IS NULL OR jp.deptId IN :deptIds)

    AND (
        jp.isLocationWise = false
        OR 
        (jp.isLocationWise = true AND (
            :stateIds IS NULL OR psd.stateId IN :stateIds
        ))
    )

    AND (
        :searchText IS NULL OR :searchText = ''
        OR LOWER(jr.requisitionCode) LIKE LOWER(CONCAT('%', :searchText, '%'))
        OR LOWER(jr.requisitionTitle) LIKE LOWER(CONCAT('%', :searchText, '%'))
        OR LOWER(mp.positionName) LIKE LOWER(CONCAT('%', :searchText, '%'))
    )

    AND (jr.startDate <=:currentDate)
    AND (jr.endDate >= :currentDate)
    AND (:appliedPositionIds IS NULL OR jp.id NOT IN :appliedPositionIds)

    AND (:monthMinExp IS NULL OR jp.mandatoryExperienceMonths >= :monthMinExp)
    AND (:monthMaxExp IS NULL OR jp.mandatoryExperienceMonths <= :monthMaxExp)
    AND (:requisitionId IS NULL OR jr.id = :requisitionId)
    ORDER BY jp.modifiedDate DESC, jp.id DESC
""")
//    Current opportunities call
    Page<JobPositionsEntity> findActiveJobPositions(
            @Param("positionStatus") PositionStatus positionStatus,
            @Param("requisitionStatus") RequisitionStatus requisitionStatus,
            @Param("candidateId") UUID candidateId,
            @Param("deptIds") List<UUID> deptIds,
            @Param("stateIds") List<UUID> stateIds,
            @Param("searchText") String searchText, // rec code , rec title, position name
            @Param("requisitionId") UUID requisitionId,
            @Param("monthMinExp") Integer monthMinExp,
            @Param("monthMaxExp") Integer monthMaxExp,
            @Param("currentDate") LocalDate currentDate,
            @Param("appliedPositionIds") List<UUID> appliedPositionsIds,
            Pageable pageable
    );


    @Query("""
                SELECT jp
                FROM JobPositionsEntity jp
                LEFT JOIN JobRequisitionsEntity jr ON jr.id = jp.requisitionId
                LEFT JOIN MasterPositionsEntity mp ON mp.id = jp.masterPositionId
                WHERE jp.id IN :positionIds
                AND (
                    :searchTerm IS NULL OR :searchTerm = ''
                    OR LOWER(jr.requisitionCode) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                    OR LOWER(mp.positionName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                )
            """)
    Page<JobPositionsEntity> findByPositionIdsWithSearch(
            @Param("positionIds") List<UUID> positionIds,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );


    @Query("""
        SELECT CONCAT(
            p.masterPositionId, '||',
            p.deptId, '||',
            p.requisitionId
        )
        FROM JobPositionsEntity p WHERE requisitionId = :requisitionId
    """)
    List<String> findAllCompositeKeysAsString(@Param("requisitionId") UUID requisitionId);


    List<JobPositionsEntity> findByRequisitionIdAndPositionStatus(UUID requisitionId, PositionStatus value);


    @Query("""
    SELECT jp FROM JobPositionsEntity jp
    JOIN MasterPositionsEntity mp ON mp.id = jp.masterPositionId
    WHERE jp.requisitionId = :requisitionId
    AND jp.positionStatus = :status
    AND (:searchText IS NULL OR LOWER(mp.positionName) LIKE LOWER(CONCAT('%', :searchText, '%')))
    """)
    List<JobPositionsEntity> searchPositions(
            @Param("requisitionId") UUID requisitionId,
            @Param("searchText") String searchText,
            @Param("status") PositionStatus status
    );

    @Query("SELECT jp.id FROM JobPositionsEntity jp WHERE jp.requisitionId IN :requisitionIds")
    List<UUID> findIdByRequisitionIdIn(@Param("requisitionIds") Collection<UUID> requisitionIds);
}
