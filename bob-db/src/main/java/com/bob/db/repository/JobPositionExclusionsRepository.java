package com.bob.db.repository;

import com.bob.db.entity.JobPositionExclusionsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobPositionExclusionsRepository extends JpaRepository<JobPositionExclusionsEntity, UUID> {
    void deleteAllByJobPositionId(UUID jobPositionId);
    List<JobPositionExclusionsEntity> findAllByJobPositionId(UUID jobPositionId);

    List<JobPositionExclusionsEntity> findAllByJobPositionIdIn(List<UUID> positionIds);
}

