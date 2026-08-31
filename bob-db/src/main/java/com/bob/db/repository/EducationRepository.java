package com.bob.db.repository;

import com.bob.db.entity.EducationEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface EducationRepository extends JpaRepository<EducationEntity, UUID> {
    List<EducationEntity> findByCandidateId(UUID candidateId);

    List<EducationEntity> findByCandidateIdAndIsSubmittedFalse(UUID candidateId);

    List<EducationEntity> findByCandidateIdAndEducationQualificationsIdIn(UUID candidateId, List<UUID> eduQualIds);

    @Modifying
    @Transactional
    @Query("UPDATE EducationEntity e SET e.isActive = false WHERE e.candidateId = :candidateId AND e.educationQualificationsId IN :qualIds")
    void softDeleteByCandidateIdAndQualificationIds(UUID candidateId, List<UUID> qualIds);
}
