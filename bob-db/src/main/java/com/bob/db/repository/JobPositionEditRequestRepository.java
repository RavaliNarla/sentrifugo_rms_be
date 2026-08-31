package com.bob.db.repository;

import com.bob.db.entity.JobPositionEditRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobPositionEditRequestRepository extends JpaRepository<JobPositionEditRequestEntity, UUID> {
    Optional<JobPositionEditRequestEntity> findByJobEditRequisition_IdAndParentPositionId(UUID jobEditRequisitionId, UUID parentPositionId);
    List<JobPositionEditRequestEntity> findAllByJobEditRequisition_IdAndParentPositionIdIn(UUID jobEditRequisitionId, Collection<UUID> parentPositionIds);
    List<JobPositionEditRequestEntity> findAllByJobEditRequisition_Id(UUID jobEditRequisitionId);

    /**
     * Hard-delete cascade helpers for re-adding a previously soft-deleted position draft.
     *
     * Must be called in order to respect FK constraints:
     *   1. hardDeleteInactiveCategoryDistribs  (references position_state_distribution_edit_requests)
     *   2. hardDeleteInactiveStateDistribs      (references job_position_edit_requests)
     *   3. hardDeleteInactiveNationalDistribs   (references job_position_edit_requests)
     *   4. hardDeleteInactiveByDraftAndParentPosition  (the position row itself)
     */
    @Modifying
    @Query(value = """
            DELETE FROM recruitment.position_category_distribution_edit_requests
            WHERE state_distribution_id IN (
                SELECT psd.id FROM recruitment.position_state_distribution_edit_requests psd
                WHERE psd.job_edit_position_id IN (
                    SELECT id FROM recruitment.job_position_edit_requests
                    WHERE job_edit_requisition_id = :draftId
                      AND parent_position_id = :parentPositionId
                      AND is_active = false
                )
            )
            """, nativeQuery = true)
    void hardDeleteInactiveCategoryDistribsByDraftAndParentPosition(
            @Param("draftId") UUID draftId,
            @Param("parentPositionId") UUID parentPositionId);

    @Modifying
    @Query(value = """
            DELETE FROM recruitment.position_state_distribution_edit_requests
            WHERE job_edit_position_id IN (
                SELECT id FROM recruitment.job_position_edit_requests
                WHERE job_edit_requisition_id = :draftId
                  AND parent_position_id = :parentPositionId
                  AND is_active = false
            )
            """, nativeQuery = true)
    void hardDeleteInactiveStateDistribsByDraftAndParentPosition(
            @Param("draftId") UUID draftId,
            @Param("parentPositionId") UUID parentPositionId);

    @Modifying
    @Query(value = """
            DELETE FROM recruitment.position_category_national_distribution_edit_requests
            WHERE job_edit_position_id IN (
                SELECT id FROM recruitment.job_position_edit_requests
                WHERE job_edit_requisition_id = :draftId
                  AND parent_position_id = :parentPositionId
                  AND is_active = false
            )
            """, nativeQuery = true)
    void hardDeleteInactiveNationalDistribsByDraftAndParentPosition(
            @Param("draftId") UUID draftId,
            @Param("parentPositionId") UUID parentPositionId);

    @Modifying
    @Query(value = """
            DELETE FROM recruitment.job_position_edit_requests
            WHERE job_edit_requisition_id = :draftId
              AND parent_position_id = :parentPositionId
              AND is_active = false
            """, nativeQuery = true)
    void hardDeleteInactiveByDraftAndParentPosition(
            @Param("draftId") UUID draftId,
            @Param("parentPositionId") UUID parentPositionId);
}
