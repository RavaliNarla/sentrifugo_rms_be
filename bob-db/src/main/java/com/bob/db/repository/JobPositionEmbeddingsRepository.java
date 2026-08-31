package com.bob.db.repository;

import com.bob.db.entity.JobPositionEmbeddingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobPositionEmbeddingsRepository extends JpaRepository<JobPositionEmbeddingsEntity, UUID> {

    Optional<JobPositionEmbeddingsEntity> findByPositionId(UUID positionId);

    List<JobPositionEmbeddingsEntity> findByPositionIdIn(List<UUID> postionsIds);
}

