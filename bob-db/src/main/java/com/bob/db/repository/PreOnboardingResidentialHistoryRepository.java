package com.bob.db.repository;

import com.bob.db.entity.PreOnboardingResidentialHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PreOnboardingResidentialHistoryRepository extends JpaRepository<PreOnboardingResidentialHistoryEntity, UUID> {

    List<PreOnboardingResidentialHistoryEntity> findByPreOnboardingId(UUID preOnboardingId);
}