package com.bob.db.repository;

import com.bob.db.entity.JobRequisitionsEntity;
import com.bob.db.enums.RequisitionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface JobRequisitionsRepositoryCustom {
    JobRequisitionsEntity saveWithAudit(JobRequisitionsEntity oldEntity, JobRequisitionsEntity newEntity);
    List<JobRequisitionsEntity> saveAllWithAudit(List<JobRequisitionsEntity> oldEntities, List<JobRequisitionsEntity> newEntities);
    
    Page<JobRequisitionsEntity> findRequisitionsWithFilters(Integer year, Integer month, List<RequisitionStatus> statuses, String searchTerm, UUID departmentId, Pageable pageable);
}
