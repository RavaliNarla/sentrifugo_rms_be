package com.bob.db.repository;

import com.bob.db.entity.ExamSectionPassMarksEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamSectionPassMarksRepository extends JpaRepository<ExamSectionPassMarksEntity, UUID> {
    List<ExamSectionPassMarksEntity> findByExamConfig_Id(UUID examConfigId);
    List<ExamSectionPassMarksEntity> findByExamConfig_IdIn(List<UUID> examConfigIds);

    Optional<ExamSectionPassMarksEntity>
    findByExamConfig_IdAndSectionNumber(
            UUID examConfigId,
            Integer sectionNumber
    );
}
