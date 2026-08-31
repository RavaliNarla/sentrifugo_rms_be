package com.bob.db.repository;

import com.bob.db.entity.CandidateSectionMarksEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateSectionMarksRepository extends JpaRepository<CandidateSectionMarksEntity, UUID> {
    List<CandidateSectionMarksEntity> findByCandidateExam_Id(UUID candidateExamId);
    List<CandidateSectionMarksEntity> findByCandidateExam_IdIn(List<UUID> candidateExamId);
    Optional<CandidateSectionMarksEntity> findByCandidateExam_IdAndExamSection_Id(UUID candidateExamId, UUID examSectionId);
    @Query("""
        SELECT DISTINCT csm
        FROM CandidateSectionMarksEntity csm
        JOIN FETCH csm.examSection es
        WHERE csm.candidateExam.id IN :examIds
    """)
    List<CandidateSectionMarksEntity> findByCandidateExam_IdInWithDetails(
            @Param("examIds") List<UUID> examIds
    );
}
