package com.bob.db.repository;

import com.bob.db.entity.CandidateConcessionsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateConcessionsRepository extends JpaRepository<CandidateConcessionsEntity, UUID> {

    List<CandidateConcessionsEntity> findAllByApplicationIdIn(List<UUID> applicationIds);
    Optional<CandidateConcessionsEntity> findByApplicationId(UUID applicationId);
}
