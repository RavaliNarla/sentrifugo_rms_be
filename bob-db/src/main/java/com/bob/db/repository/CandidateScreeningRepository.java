package com.bob.db.repository;

import com.bob.db.entity.CandidateScreeningEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateScreeningRepository extends JpaRepository<CandidateScreeningEntity, UUID>{

    Optional<CandidateScreeningEntity> findByApplicationId(UUID applicationId);

    List<CandidateScreeningEntity> findByCandidateId(UUID candidateId);

    List<CandidateScreeningEntity> findAllByApplicationIdIn(List<UUID> applicationIds);
}

