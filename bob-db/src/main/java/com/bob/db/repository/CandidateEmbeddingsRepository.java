package com.bob.db.repository;

import com.bob.db.entity.CandidateEmbeddingsEntity;
import com.bob.db.entity.CandidateLocationPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateEmbeddingsRepository  extends JpaRepository<CandidateEmbeddingsEntity, UUID> {

    Optional<CandidateEmbeddingsEntity> findByApplicationId(UUID applicationId);
}