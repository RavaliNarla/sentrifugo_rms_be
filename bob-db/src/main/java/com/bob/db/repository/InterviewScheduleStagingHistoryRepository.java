package com.bob.db.repository;

import com.bob.db.entity.InterviewScheduleStagingEntity;
import com.bob.db.entity.InterviewScheduleStagingHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewScheduleStagingHistoryRepository extends JpaRepository<InterviewScheduleStagingHistoryEntity, UUID>, JpaSpecificationExecutor<InterviewScheduleStagingHistoryEntity> {
    List<InterviewScheduleStagingHistoryEntity> findByApplicationIdIn(List<UUID> allApplicationIds);
}
