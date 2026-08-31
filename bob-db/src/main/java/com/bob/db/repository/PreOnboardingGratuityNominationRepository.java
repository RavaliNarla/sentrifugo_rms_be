package com.bob.db.repository;

import com.bob.db.entity.PreOnboardingGratuityNominationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreOnboardingGratuityNominationRepository extends JpaRepository<PreOnboardingGratuityNominationEntity, UUID> {

    Optional<PreOnboardingGratuityNominationEntity> findByPreOnboardingId(UUID preOnboardingId);
}