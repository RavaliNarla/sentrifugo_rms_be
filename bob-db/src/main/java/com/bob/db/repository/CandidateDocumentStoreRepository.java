package com.bob.db.repository;

import com.bob.db.entity.CandidateDocumentStoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateDocumentStoreRepository extends JpaRepository<CandidateDocumentStoreEntity, UUID> {
    Optional<CandidateDocumentStoreEntity> findByCandidateIdAndDocumentId(UUID candidateId, UUID documentId);

    Optional<CandidateDocumentStoreEntity> findByCandidateIdAndDocumentIdAndDocumentNumber(UUID candidateId, UUID documentId, String documentNumber);

    List<CandidateDocumentStoreEntity> findAllByCandidateId(UUID candidateId);

    Optional<CandidateDocumentStoreEntity> findByCandidateIdAndDocumentIdAndFileName(UUID candidateId, UUID documentId, String fileName);


    List<CandidateDocumentStoreEntity> findByCandidateIdAndDocumentIdentifierIn(UUID candidateId, List<UUID> documentIdentifiers);

    List<CandidateDocumentStoreEntity> findByDocumentIdentifier(UUID workExperienceId);

    Optional<CandidateDocumentStoreEntity> findByCandidateIdAndDocumentIdentifier(UUID candidateId, UUID id);

    List<CandidateDocumentStoreEntity> findAllByDocumentIdAndCandidateIdIn(UUID id, List<UUID> candidateIds);

    /**
     * Checks if candidate has uploaded at least one document with specified doc_type
     * by joining candidate_document_store with document_types table
     *
     * @param candidateId The candidate UUID
     * @param docType The doc_type value to check (e.g., "idverification")
     * @return true if at least one matching document exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(cds) > 0 THEN true ELSE false END " +
           "FROM CandidateDocumentStoreEntity cds " +
           "JOIN DocumentTypesEntity dt ON cds.documentId = dt.id " +
           "WHERE cds.candidateId = :candidateId " +
           "AND dt.docType = :docType")
    boolean existsByCandidateIdAndDocType(@Param("candidateId") UUID candidateId, 
                                          @Param("docType") String docType);
}
