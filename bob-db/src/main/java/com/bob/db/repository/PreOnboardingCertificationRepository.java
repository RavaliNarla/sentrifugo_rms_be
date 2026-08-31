package com.bob.db.repository;

import com.bob.db.entity.PreOnboardingCertificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PreOnboardingCertificationRepository extends JpaRepository<PreOnboardingCertificationEntity, UUID> {

    List<PreOnboardingCertificationEntity> findByPreOnboardingId(UUID preOnboardingId);
}