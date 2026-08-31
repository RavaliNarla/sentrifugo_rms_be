package com.bob.db.repository;

import com.bob.db.entity.CandidateDisabilityDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateDisabilityDetailsRepository extends JpaRepository<CandidateDisabilityDetailsEntity, UUID> {
    Optional<CandidateDisabilityDetailsEntity> findByCandidateId(UUID candidateId);
    List<CandidateDisabilityDetailsEntity> findAllByCandidateId(UUID candidateId);

    List<CandidateDisabilityDetailsEntity> findAllByCandidateIdIn(List<UUID> candidateIds);

}

