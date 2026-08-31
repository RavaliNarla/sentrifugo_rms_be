package com.bob.db.repository;

import com.bob.db.entity.PreOnboardingQualificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PreOnboardingQualificationRepository extends JpaRepository<PreOnboardingQualificationEntity, UUID> {

    List<PreOnboardingQualificationEntity> findByPreOnboardingId(UUID preOnboardingId);

}