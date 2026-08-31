package com.bob.db.repository;

import com.bob.db.entity.JobPositionExclusionsHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobPositionExclusionsHistoryRepository extends JpaRepository<JobPositionExclusionsHistoryEntity, UUID> {
}
