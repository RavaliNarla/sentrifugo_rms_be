package com.bob.db.repository;

import com.bob.db.entity.CandidateWrittenExamMarksEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateWrittenExamMarksRepository extends JpaRepository<CandidateWrittenExamMarksEntity, UUID> {
    List<CandidateWrittenExamMarksEntity> findByExamConfig_Id(UUID examConfigId);
    List<CandidateWrittenExamMarksEntity> findByPosition_Id(UUID positionId);
    List<CandidateWrittenExamMarksEntity> findByPosition_IdIn(List<UUID> positionIds);
    Optional<CandidateWrittenExamMarksEntity> findByApplication_IdAndExamConfig_Id(UUID applicationId, UUID examConfigId);
    List<CandidateWrittenExamMarksEntity> findAllByApplication_IdInAndExamConfig_IdIn(List<UUID> applicationIds,List<UUID> examConfigIds);
    List<CandidateWrittenExamMarksEntity> findByApplication_IdIn(List<UUID> applicationId);
    @Query("""
        SELECT cwm
        FROM CandidateWrittenExamMarksEntity cwm
        JOIN FETCH cwm.candidate
        JOIN FETCH cwm.application
        JOIN FETCH cwm.position
        JOIN FETCH cwm.examConfig
        WHERE cwm.position.id IN :positionIds
        """)
    List<CandidateWrittenExamMarksEntity> findAllByPositionIdsWithDetails(@Param("positionIds") List<UUID> positionIds);
}
