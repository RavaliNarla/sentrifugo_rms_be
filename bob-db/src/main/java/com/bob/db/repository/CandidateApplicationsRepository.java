package com.bob.db.repository;

import com.bob.db.entity.CandidateApplicationsEntity;
import com.bob.db.enums.CandidateApplicationStatus;
import com.bob.db.model.CandidateApplicationWithRankProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
public interface CandidateApplicationsRepository extends JpaRepository<CandidateApplicationsEntity, UUID>,CandidateApplicationsRepositoryCustom {

    List<CandidateApplicationsEntity> findByPositionId(UUID positionId);

    List<CandidateApplicationsEntity> findAllByPositionIdIn(List<UUID> positionIds);

    List<CandidateApplicationsEntity> findByCandidateIdOrderByApplicationDateDesc(UUID candidateId);

    Optional<CandidateApplicationsEntity> findByCandidateIdAndPositionId(UUID candidateId, UUID positionId);

    List<CandidateApplicationsEntity> findByApplicationStatus( CandidateApplicationStatus status);

    List<CandidateApplicationsEntity> findByCandidateIdInAndPositionId(List<UUID> candidateIds, UUID positionId);

    List<CandidateApplicationsEntity> findTop5ByCandidateIdOrderByCreatedDateDesc(UUID candidateId);

    List<CandidateApplicationsEntity> findByCandidateIdAndPositionIdIn(UUID candidateId, List<UUID> positionIds);

    List<CandidateApplicationsEntity> findAllByApplicationStatusIn(List<CandidateApplicationStatus> statusList);

    List<CandidateApplicationsEntity> findByCandidateId(UUID candidateId);

    @Query("SELECT COUNT(ca) > 0 FROM CandidateApplicationsEntity ca " +
           "INNER JOIN JobPositionsEntity jp ON ca.positionId = jp.id " +
           "WHERE ca.candidateId = :candidateId AND jp.requisitionId = :requisitionId")
    boolean existsByCandidateIdAndRequisitionId(@Param("candidateId") UUID candidateId,
                                                  @Param("requisitionId") UUID requisitionId);

    @Query("""
        SELECT c
        FROM CandidateApplicationsEntity c
        JOIN CandidateProfileEntity p ON p.candidateId = c.candidateId
        JOIN CandidateLocationPreferenceEntity a ON a.candidateId = c.candidateId and a.positionId = c.positionId
        LEFT JOIN CandidateRankingResultsEntity r ON r.applicationId = c.id
        WHERE c.positionId IN :positionIds
          AND c.isActive = true
          AND (:status IS NULL OR c.applicationStatus IN :status)
          AND (CAST(:categoryId AS string) IS NULL OR CAST(:categoryId AS string) = '' OR p.reservationCategoryId = :categoryId)
          AND (CAST(:stateId AS string) IS NULL OR CAST(:stateId AS string) = '' OR a.statePreference1 = :stateId)
          AND (
              :searchText IS NULL OR :searchText = ''
              OR LOWER(
                  FUNCTION('concat_ws',' ', p.firstName, p.middleName, p.lastName)
              ) LIKE LOWER(CONCAT('%', :searchText, '%'))
          )
        ORDER BY c.positionId ASC,
          CASE WHEN :rank = true THEN r.finalScore END DESC
        """)
    Page<CandidateApplicationsEntity> findAppliedCandidatesByPosition(
            @Param("searchText") String searchText,
            @Param("positionIds") List<UUID> positionIds,
            @Param("status") List<CandidateApplicationStatus> status,
            @Param("stateId") UUID stateId,
            @Param("categoryId") UUID categoryId,
            @Param("rank") Boolean rank,
            Pageable pageable
    );

    @Query("""
            SELECT c
            FROM CandidateApplicationsEntity c
            JOIN CandidateProfileEntity p ON c.candidateId = p.candidateId
            JOIN CandidateLocationPreferenceEntity l ON l.candidateId = c.candidateId and c.positionId = l.positionId
            WHERE c.positionId IN (:positionIds)
            AND (:status IS NULL OR c.applicationStatus IN :status)
            AND (CAST(:categoryId AS string) IS NULL OR CAST(:categoryId AS string) = '' OR p.reservationCategoryId =:categoryId)
            AND (CAST(:stateId AS string) IS NULL OR CAST(:stateId AS string) = '' OR l.statePreference1 = :stateId)
            ORDER BY c.applicationNo ASC
           """)
    List<CandidateApplicationsEntity> findAllAppliedCandidatesByPositionId(
            @Param("positionIds") List<UUID> positionIds,
            @Param("status") List<CandidateApplicationStatus> status,
            @Param("stateId") UUID stateId,
            @Param("categoryId") UUID categoryId

    );



    @Query(value = """
    SELECT
        c.id as id,
        c.candidate_id as candidateId,
        c.position_id as positionId,

        CASE
            WHEN :rank = true THEN
                ROW_NUMBER() OVER (
                    PARTITION BY c.position_id
                    ORDER BY r.final_score DESC, c.id
                )
            ELSE NULL
        END AS rankNumber

    FROM candidate.candidate_applications c 

    JOIN candidate.candidate_profile p
        ON p.candidate_id = c.candidate_id

    JOIN candidate.candidate_location_preference a
        ON a.candidate_id = c.candidate_id
        AND a.position_id = c.position_id

    LEFT JOIN recruitment.candidate_ranking_results r
        ON r.application_id = c.id

    WHERE c.position_id IN (:positionIds)
      AND c.is_active = true
      AND c.payment_status = 'SUCCESS'

      AND (
            :status IS NULL
            OR c.application_status IN (:status)
      )

      AND (
            :categoryId IS NULL
            OR p.reservation_category_id = :categoryId
      )

      AND (
            :stateId IS NULL
            OR a.state_preference1 = :stateId
      )

      AND (
          :searchText IS NULL
          OR :searchText = ''
          OR LOWER(CONCAT(
                COALESCE(p.first_name,''), ' ',
                COALESCE(p.middle_name,''), ' ',
                COALESCE(p.last_name,'')
          )) LIKE LOWER(CONCAT('%', :searchText, '%'))
      )

    ORDER BY
        c.position_id,
        rankNumber,
        c.id
    """,
            countQuery  = """
    SELECT COUNT(DISTINCT c.id)

    FROM candidate.candidate_applications c 

    JOIN candidate.candidate_profile p
        ON p.candidate_id = c.candidate_id

    JOIN candidate.candidate_location_preference a
        ON a.candidate_id = c.candidate_id
       AND a.position_id = c.position_id

    WHERE c.position_id IN (:positionIds)
      AND c.payment_status = 'SUCCESS'
      AND c.is_active = true

      AND (
            :status IS NULL
            OR c.application_status IN (:status)
      )

      AND (
            :categoryId IS NULL
            OR p.reservation_category_id = :categoryId
      )

      AND (
            :stateId IS NULL
            OR a.state_preference1 = :stateId
      )

      AND (
            :searchText IS NULL
            OR :searchText = ''
            OR LOWER(CONCAT(
                    COALESCE(p.first_name, ''), ' ',
                    COALESCE(p.middle_name, ''), ' ',
                    COALESCE(p.last_name, '')
               )) LIKE LOWER(CONCAT('%', :searchText, '%'))
      )
""",
            nativeQuery = true)
    Page<CandidateApplicationWithRankProjection>
    findAppliedCandidatesByPositionWithRank(
            @Param("searchText") String searchText,
            @Param("positionIds") List<UUID> positionIds,
            @Param("status") List<String> status,
            @Param("stateId") UUID stateId,
            @Param("categoryId") UUID categoryId,
            @Param("rank") Boolean rank,
            Pageable pageable
    );

    List<CandidateApplicationsEntity> findAllByIdIn(List<UUID> applicationIds);

    List<CandidateApplicationsEntity> findAllByIdInAndApplicationStatus(List<UUID> listOfApplicationsIds, CandidateApplicationStatus candidateApplicationStatus);

    List<CandidateApplicationsEntity> findByPositionIdIn(Collection<UUID> positionIds);

    // Native query bypasses the @Where filter on CandidateApplicationsEntity, which
    // would otherwise block PENDING -> SUCCESS updates.
    @Modifying
    @Transactional
    @Query(value = "UPDATE candidate.candidate_applications SET payment_status = :status WHERE id = :id", nativeQuery = true)
    void updatePaymentStatus(@Param("id") UUID id, @Param("status") String status);

    // Native query bypasses the @Where clause (is_active = true AND payment_status = 'SUCCESS')
    // so that applications in all payment states (PENDING, FAILED, SUCCESS) are returned.
    @Query(value = "SELECT * FROM candidate.candidate_applications WHERE candidate_id = :candidateId AND is_active = true", nativeQuery = true)
    List<CandidateApplicationsEntity> findByCandidateIdIgnoringPaymentStatus(@Param("candidateId") UUID candidateId);

    @Query(value = """
            SELECT * 
            FROM candidate.candidate_applications
            WHERE id = :applicationId
            AND is_active = true
            """, nativeQuery = true)
    Optional<CandidateApplicationsEntity> findByApplicationIdIgnoringPaymentStatus(
            @Param("applicationId") UUID applicationId
    );

    List<CandidateApplicationsEntity> findByPositionIdAndApplicationStatus(UUID positionId, CandidateApplicationStatus candidateApplicationStatus);

    boolean existsByCandidateIdAndPositionId(UUID candidateId, UUID positionId);

    Boolean existsByPositionIdAndApplicationStatusNotIn(UUID positionId, List<CandidateApplicationStatus> allowedStatuses);
}
