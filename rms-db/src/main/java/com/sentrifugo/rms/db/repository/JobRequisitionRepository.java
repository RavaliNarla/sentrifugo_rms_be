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
}
