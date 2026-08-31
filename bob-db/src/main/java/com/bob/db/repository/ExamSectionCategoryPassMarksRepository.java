package com.bob.db.repository;

import com.bob.db.entity.ExamSectionCategoryPassMarksEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamSectionCategoryPassMarksRepository extends JpaRepository<ExamSectionCategoryPassMarksEntity, UUID> {
    List<ExamSectionCategoryPassMarksEntity> findByExamSection_Id(UUID examSectionId);
    Optional<ExamSectionCategoryPassMarksEntity>
    findByExamSection_IdAndCategory_IdAndStateId(
            UUID examSectionId,
            UUID categoryId,
            UUID stateId
    );

    Optional<ExamSectionCategoryPassMarksEntity>
    findByExamSection_IdAndCategory_IdAndStateIdIsNull(
            UUID examSectionId,
            UUID categoryId
    );

    @Query("""
        SELECT DISTINCT escpm
        FROM ExamSectionCategoryPassMarksEntity escpm
        JOIN FETCH escpm.examSection es
        JOIN FETCH escpm.category
        WHERE es.id IN :sectionIds
    """)
    List<ExamSectionCategoryPassMarksEntity> findAllByExamSectionIdsWithDetails(
            @Param("sectionIds") List<UUID> sectionIds
    );
}
