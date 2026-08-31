package com.bob.db.repository;

import com.bob.db.entity.WorkExperienceEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkExperienceRepository extends JpaRepository<WorkExperienceEntity, UUID> {
    List<WorkExperienceEntity> findByCandidateId(UUID candidateId);

    List<WorkExperienceEntity> findByCandidateId(UUID candidateId, Sort sort);

    List<WorkExperienceEntity> findByCandidateIdOrderByFromDateDesc(UUID candidateId);
    @Query("""
    SELECT COUNT(w) > 0
    FROM WorkExperienceEntity w
    WHERE w.candidateId = :candidateId
      AND :fromDate <= COALESCE(w.toDate, :maxDate)
      AND COALESCE(:toDate, :maxDate) >= w.fromDate
""")
    boolean existsOverlappingExperience(
            UUID candidateId,
            LocalDate fromDate,
            LocalDate toDate,
            LocalDate maxDate
    );

    List<WorkExperienceEntity> findAllByCandidateIdIn(List<UUID> candidateIds);
}
