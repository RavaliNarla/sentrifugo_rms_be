package com.bob.db.repository;


import com.bob.db.entity.PreOnboardingDocumentEntity;
import com.bob.db.entity.PreOnboardingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreOnboardingRepository extends JpaRepository<PreOnboardingEntity, UUID> {

    Optional<PreOnboardingEntity> findByApplicationId(UUID applicationId);

}