package com.bob.db.repository;

import com.bob.db.entity.QualificationGroupMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface QualificationGroupMappingRepository extends JpaRepository<QualificationGroupMappingEntity, UUID> {

    List<QualificationGroupMappingEntity> findByEducationQualificationIdIn(List<UUID> educationQualificationIds);

    @Modifying
    @Query("DELETE FROM QualificationGroupMappingEntity q WHERE q.educationQualificationId = :qualificationId")
    void deleteByEducationQualificationId(UUID qualificationId);

    @Query("""
    SELECT q
    FROM QualificationGroupMappingEntity q
    WHERE q.educationQualificationId = :qualificationId
      AND (
            (:specializationId IS NULL AND q.specializationId IS NULL)
            OR q.specializationId = :specializationId
          )
    """)
    Optional<QualificationGroupMappingEntity> findMapping(@Param("qualificationId") UUID qualificationId, @Param("specializationId") UUID specializationId);


}