package com.bob.db.repository;

import com.bob.db.entity.CandidateExamSectionMarksEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateExamSectionMarksRepository extends JpaRepository<CandidateExamSectionMarksEntity, UUID> {
    List<CandidateExamSectionMarksEntity> findByApplication_Id(UUID applicationId);
    List<CandidateExamSectionMarksEntity> findByCandidate_IdAndExamConfig_Id(UUID candidateId, UUID examConfigId);
}
