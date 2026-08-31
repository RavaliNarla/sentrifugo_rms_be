package com.bob.db.repository;

import com.bob.db.entity.PreOnboardingAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreOnboardingAddressRepository extends JpaRepository<PreOnboardingAddressEntity, UUID> {

    Optional<PreOnboardingAddressEntity> findByPreOnboardingId(UUID preOnboardingId);
}