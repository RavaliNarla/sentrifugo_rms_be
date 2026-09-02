package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.CandidateOfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateOfferRepository extends JpaRepository<CandidateOfferEntity, UUID> {
    Optional<CandidateOfferEntity> findByCandidateId(UUID candidateId);
    Optional<CandidateOfferEntity> findByAcceptToken(UUID acceptToken);
    List<CandidateOfferEntity> findByCandidateIdIn(List<UUID> candidateIds);
}
