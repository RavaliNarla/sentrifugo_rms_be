package com.bob.db.repository;

import com.bob.db.entity.InterviewPanelsEntity;
import com.bob.db.entity.JobRequisitionsEntity;
import com.bob.db.enums.RequisitionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRequisitionsRepository extends JpaRepository<JobRequisitionsEntity, UUID>, JobRequisitionsRepositoryCustom, JpaSpecificationExecutor<JobRequisitionsEntity> {

    // Pessimistic write lock for publish path; prevents concurrent edits/publishes on same requisition row.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT jr FROM JobRequisitionsEntity jr WHERE jr.id = :id")
    Optional<JobRequisitionsEntity> findByIdForUpdate(@Param("id") UUID id);

    // Find by status
    List<JobRequisitionsEntity> findAllByRequisitionStatus(String status);

    // Find by multiple statuses
    List<JobRequisitionsEntity> findAllByRequisitionStatusIn(Collection<RequisitionStatus> requisitionStatuses);

    // Find by IDs
    List<JobRequisitionsEntity> findAllByIdIn(List<UUID> requisitionIds);

    List<JobRequisitionsEntity> findAllByRequisitionStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            RequisitionStatus status,
            LocalDate today1,
            LocalDate today2
    );
    @Query("""
       SELECT jr FROM JobRequisitionsEntity jr
       WHERE jr.requisitionStatus = :status
       AND jr.startDate <= :today
       AND jr.endDate >= :today
       AND (:searchTerm IS NULL OR LOWER(jr.requisitionTitle) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
       """)
    List<JobRequisitionsEntity> searchApprovedRequisitions(
            @Param("searchTerm") String searchTerm,
            @Param("status") RequisitionStatus status,
            @Param("today") LocalDate today
    );
    @Query("SELECT DISTINCT j.startDate FROM JobRequisitionsEntity j ORDER BY j.startDate DESC")
    List<LocalDate> getYears();
}
