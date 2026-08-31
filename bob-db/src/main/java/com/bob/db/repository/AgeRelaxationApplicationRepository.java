package com.bob.db.repository;

import com.bob.db.entity.AgeRelaxationApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgeRelaxationApplicationRepository extends JpaRepository<AgeRelaxationApplicationEntity, UUID> {
    
    Optional<AgeRelaxationApplicationEntity> findByCandidateIdAndPositionId(UUID candidateId, UUID positionId);
    
    Optional<AgeRelaxationApplicationEntity> findByCandidateIdAndPositionIdAndStateIdAndCityId(
            UUID candidateId, UUID positionId, UUID stateId, UUID cityId);

    boolean existsByCandidateIdAndPositionIdAndStateIdAndStateAgeValidationPassedTrue(
            UUID candidateId, UUID positionId, UUID stateId);

    AgeRelaxationApplicationEntity findTopByPositionIdAndCandidateIdOrderByCreatedDateDesc(UUID positionId,UUID candidateId);
}
