package com.bob.db.repository;

import com.bob.db.entity.PreOnboardingReferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PreOnboardingReferenceRepository
        extends JpaRepository<PreOnboardingReferenceEntity, UUID> {

    List<PreOnboardingReferenceEntity> findByPreOnboardingId(
            UUID preOnboardingId
    );
}