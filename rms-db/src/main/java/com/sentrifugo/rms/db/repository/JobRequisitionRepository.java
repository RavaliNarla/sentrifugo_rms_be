package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.JobRequisitionEntity;
import com.sentrifugo.rms.db.enums.RequisitionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRequisitionRepository extends JpaRepository<JobRequisitionEntity, UUID> {

    Page<JobRequisitionEntity> findAllByOrderByCreatedDateDesc(Pageable pageable);

    List<JobRequisitionEntity> findByStatusIn(List<RequisitionStatus> statuses);

    List<JobRequisitionEntity> findByStatus(RequisitionStatus status);

    @Query("SELECT r FROM JobRequisitionEntity r WHERE r.status = :status ORDER BY r.title ASC")
    List<JobRequisitionEntity> findApprovedForDropdown(@Param("status") RequisitionStatus status);

    @Query(value = "SELECT nextval('recruitment.requisition_code_seq')", nativeQuery = true)
    Long nextRequisitionCodeSeq();

    // Drives the Job Postings screen: server-side search/filter, paginated. Job title/department/location
    // filters match if ANY position on the requisition has that value (mirrors the old client-side filter,
    // which checked each of the three independently rather than requiring one position to match all three).
    @Query("SELECT r FROM JobRequisitionEntity r WHERE " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:search IS NULL OR :search = '' OR LOWER(r.requisitionCode) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:yearFrom IS NULL OR YEAR(r.startDate) >= :yearFrom) AND " +
            "(:yearTo IS NULL OR YEAR(r.startDate) <= :yearTo) AND " +
            "(:month IS NULL OR MONTH(r.startDate) = :month) AND " +
            "(:jobTitle IS NULL OR EXISTS (SELECT 1 FROM JobPositionEntity p JOIN PositionTitleEntity pt ON pt.id = p.positionTitleId WHERE p.requisitionId = r.id AND pt.name = :jobTitle)) AND " +
            "(:department IS NULL OR EXISTS (SELECT 1 FROM JobPositionEntity p JOIN DepartmentEntity d ON d.id = p.departmentId WHERE p.requisitionId = r.id AND d.name = :department)) AND " +
            "(:location IS NULL OR EXISTS (SELECT 1 FROM JobPositionEntity p JOIN LocationEntity l ON l.id = p.locationId WHERE p.requisitionId = r.id AND l.name = :location))")
    Page<JobRequisitionEntity> search(
            @Param("search") String search,
            @Param("status") RequisitionStatus status,
            @Param("yearFrom") Integer yearFrom,
            @Param("yearTo") Integer yearTo,
            @Param("month") Integer month,
            @Param("jobTitle") String jobTitle,
            @Param("department") String department,
            @Param("location") String location,
            Pageable pageable);

    @Query("SELECT DISTINCT YEAR(r.startDate) FROM JobRequisitionEntity r WHERE r.startDate IS NOT NULL ORDER BY 1 DESC")
    List<Integer> findDistinctStartYears();

    // Drives the Requisition Approvals (L1/L2) screen: server-side search/status-filter, paginated,
    // newest first. allowedStatuses is the fixed set of statuses visible at that approval level;
    // status further narrows within that set when the approver picks a specific status filter.
    @Query("SELECT r FROM JobRequisitionEntity r WHERE r.status IN :allowedStatuses AND " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:search IS NULL OR :search = '' OR LOWER(r.requisitionCode) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<JobRequisitionEntity> searchForApproval(
            @Param("allowedStatuses") List<RequisitionStatus> allowedStatuses,
            @Param("status") RequisitionStatus status,
            @Param("search") String search,
            Pageable pageable);
}
