package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.SpecializationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpecializationRepository extends JpaRepository<SpecializationEntity, UUID> {
    List<SpecializationEntity> findAllByOrderByNameAsc();

    List<SpecializationEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    /** Specializations tied to this education level, plus any general (unlinked) ones. */
    @Query("""
        SELECT s FROM SpecializationEntity s
        WHERE s.educationQualificationId = :educationQualificationId OR s.educationQualificationId IS NULL
        ORDER BY s.name ASC
        """)
    List<SpecializationEntity> findByEducationQualificationIdOrGeneral(@Param("educationQualificationId") UUID educationQualificationId);

    boolean existsByNameIgnoreCase(String name);
}
