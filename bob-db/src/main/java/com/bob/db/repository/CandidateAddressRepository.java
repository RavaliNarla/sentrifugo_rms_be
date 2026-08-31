package com.bob.db.repository;

import com.bob.db.entity.CandidateAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateAddressRepository extends JpaRepository<CandidateAddressEntity, UUID> {
    Optional<CandidateAddressEntity> findByCandidateId(UUID candidateId);

    List<CandidateAddressEntity> findAllByCandidateIdIn(List<UUID> candidateIds);
}
