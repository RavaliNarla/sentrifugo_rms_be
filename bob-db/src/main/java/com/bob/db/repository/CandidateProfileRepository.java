package com.bob.db.repository;

import com.bob.db.entity.CandidateProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfileEntity, UUID> {
    Optional<CandidateProfileEntity> findByCandidateId(UUID candidateId);

    List<CandidateProfileEntity> findAllByCandidateIdIn(List<UUID> candidateIds);

    List<CandidateProfileEntity> findByCandidateIdIn(Collection<UUID> candidateIds);
}
