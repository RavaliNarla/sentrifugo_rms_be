package com.bob.db.repository;

import com.bob.db.entity.PreOnboardingPersonalDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreOnboardingPersonalDetailsRepository extends JpaRepository<PreOnboardingPersonalDetailsEntity, UUID> {


    Optional<PreOnboardingPersonalDetailsEntity> findByPreOnboardingId(UUID preOnboardingId);
}