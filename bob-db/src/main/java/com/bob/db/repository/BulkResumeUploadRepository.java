package com.bob.db.repository;

import com.bob.db.entity.BulkResumeUploadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BulkResumeUploadRepository extends JpaRepository<BulkResumeUploadEntity, UUID> {
    List<BulkResumeUploadEntity> findAllByCreatedByOrderByCreatedDateDesc(UUID createdBy);

    List<BulkResumeUploadEntity> findAllByStatusAndCreatedBy(BulkResumeUploadEntity.Status status, UUID createdBy);
}
