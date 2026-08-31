package com.bob.db.repository;

import com.bob.db.entity.CandidateCompensationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface CandidateCompensationRepository extends JpaRepository<CandidateCompensationEntity, UUID>, JpaSpecificationExecutor<CandidateCompensationEntity> {
    Optional<CandidateCompensationEntity> findByApplicationIdAndCandidateId(UUID applicationId, UUID candidateId);

    List<CandidateCompensationEntity> findByInterviewScheduleIdIn(List<UUID> interviewIds);

    @Query("""
            SELECT cc
            FROM CandidateCompensationEntity cc
            JOIN FETCH cc.candidateProfile cp
            JOIN FETCH cc.application ca
            WHERE cc.id IN :ids
            """)
    List<CandidateCompensationEntity> findAllWithCandidateProfileAndApplication(List<UUID> ids);

    List<CandidateCompensationEntity> findByApplicationIdIn(List<UUID> applicationIds);
    Optional<CandidateCompensationEntity> findByApplicationId(UUID applicationId);
}
