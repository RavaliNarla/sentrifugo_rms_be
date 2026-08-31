package com.bob.db.repository;

import com.bob.db.entity.CandidateLocationPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface CandidateLocationPreferencesRepository extends JpaRepository<CandidateLocationPreferenceEntity, UUID>, JpaSpecificationExecutor<CandidateLocationPreferenceEntity> {
    Optional<CandidateLocationPreferenceEntity> findByCandidateIdAndPositionId(UUID candidateId, UUID positionId);
    @Query("SELECT p FROM CandidateLocationPreferenceEntity p " +
            "WHERE p.candidateId IN :cIds AND p.positionId IN :pIds")
    List<CandidateLocationPreferenceEntity> findByCandidateAndPosition(
            @Param("cIds") List<UUID> candidateIds,
            @Param("pIds") List<UUID> positionIds
    );

    List<CandidateLocationPreferenceEntity> findByCandidateIdAndPositionIdIn(UUID candidateId, List<UUID> positionIds);

    List<CandidateLocationPreferenceEntity> findAllByCandidateIdInAndPositionIdIn(List<UUID> candidateIds, List<UUID> positionIds);

    List<CandidateLocationPreferenceEntity> findAllByPositionId(UUID positionId);
}
