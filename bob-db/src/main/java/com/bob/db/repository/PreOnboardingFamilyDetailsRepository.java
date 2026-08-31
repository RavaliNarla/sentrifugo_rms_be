package com.bob.db.repository;

import com.bob.db.entity.PreOnboardingFamilyDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreOnboardingFamilyDetailsRepository extends JpaRepository<PreOnboardingFamilyDetailsEntity, UUID> {

    Optional<PreOnboardingFamilyDetailsEntity> findByPreOnboardingId(UUID preOnboardingId);
}