package com.bob.db.repository;

import com.bob.db.entity.CandidateRankingResultsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateRankingResultsRepository extends JpaRepository<CandidateRankingResultsEntity, UUID> {

    List<CandidateRankingResultsEntity> findByCandidateId(UUID candidateId);

    Optional<CandidateRankingResultsEntity> findByApplicationId(UUID applicationId);
    List<CandidateRankingResultsEntity> findByApplicationIdIn(List<UUID> applicationIds);

}

