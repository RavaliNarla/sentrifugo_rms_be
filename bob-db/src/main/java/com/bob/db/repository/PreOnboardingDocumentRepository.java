package com.bob.db.repository;

import com.bob.db.entity.PreOnboardingDocumentEntity;
import com.bob.db.enums.PreOnboardingDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreOnboardingDocumentRepository extends JpaRepository<PreOnboardingDocumentEntity, UUID> {

    List<PreOnboardingDocumentEntity> findByPreOnboardingIdAndReferenceIdIn(UUID preOnboardingId,List<UUID> referenceIds);

    List<PreOnboardingDocumentEntity> findByPreOnboardingId(UUID id);

    List<PreOnboardingDocumentEntity> findByPreOnboardingIdAndReferenceTableIn(UUID preOnboardingId, List<String> referenceTables);

    List<PreOnboardingDocumentEntity> findByPreOnboardingIdAndDocumentTypeIn(UUID preOnboardingId,List<PreOnboardingDocumentType> documentTypes);
}