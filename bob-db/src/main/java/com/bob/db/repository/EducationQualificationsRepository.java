package com.bob.db.repository;

import com.bob.db.entity.EducationQualificationsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EducationQualificationsRepository extends JpaRepository<EducationQualificationsEntity, UUID> {
    List<EducationQualificationsEntity> findAllByIsActiveTrue();
    List<EducationQualificationsEntity> findByQualificationCodeIn(List<String> qualificationCodes);
    List<EducationQualificationsEntity> findAllByLevelIdIn(List<UUID> educationLevelIds);
    List<EducationQualificationsEntity> findByLevelIdIn(List<UUID> levelIds);
}

