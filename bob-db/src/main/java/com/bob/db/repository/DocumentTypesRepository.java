package com.bob.db.repository;

import com.bob.db.entity.DocumentTypesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface DocumentTypesRepository extends JpaRepository<DocumentTypesEntity, UUID> {
    List<DocumentTypesEntity> findAllByIsActiveTrue();

    List<DocumentTypesEntity> findByDocType(String docType);

    DocumentTypesEntity findByDocumentName(String documentName);

    DocumentTypesEntity findByDocCode(String docCode);

    List<DocumentTypesEntity> findByDocCodeIn(Collection<String> docCodes);

    List<DocumentTypesEntity> findByIsRequiredTrue();

    boolean existsById(UUID uuid);

    List<DocumentTypesEntity> findByDocTypeIn(List<String> docTypes);
}
