package com.bob.db.repository;

import com.bob.db.entity.PreOnboardingDomicileDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreOnboardingDomicileDetailsRepository extends JpaRepository<PreOnboardingDomicileDetailsEntity, UUID> {

    Optional<PreOnboardingDomicileDetailsEntity> findByPreOnboardingId(UUID preOnboardingId);
}