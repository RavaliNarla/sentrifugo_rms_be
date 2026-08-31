package com.bob.db.repository;

import com.bob.db.entity.JobPositionsHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobPositionsHistoryRepository extends JpaRepository<JobPositionsHistoryEntity, UUID> {
}
