package com.bob.db.repository;

import com.bob.db.entity.PreOnboardingDisabilityDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreOnboardingDisabilityDetailsRepository extends JpaRepository<PreOnboardingDisabilityDetailsEntity, UUID> {

    List<PreOnboardingDisabilityDetailsEntity> findByPreOnboardingId(UUID preOnboardingId);
}