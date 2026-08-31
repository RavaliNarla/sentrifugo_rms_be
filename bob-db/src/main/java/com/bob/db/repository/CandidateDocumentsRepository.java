package com.bob.db.repository;

import com.bob.db.entity.CandidateDocumentsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface CandidateDocumentsRepository extends JpaRepository<CandidateDocumentsEntity, UUID> {


    Optional<CandidateDocumentsEntity> findByDocumentTypeAndApplicationId(String documentType, UUID applicationId);
}
