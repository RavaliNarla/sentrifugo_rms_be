package com.bob.jobportal.service;

import com.bob.db.dto.CandidateApplicationDocumentVerificationDTO;
import com.bob.db.entity.CandidateApplicationsEntity;
import com.bob.db.entity.CandidateApplicationDocumentVerificationEntity;
import com.bob.db.entity.DocumentTypesEntity;
import com.bob.db.entity.UserEntity;
import com.bob.db.enums.DocumentScreeningStatus;
import com.bob.db.enums.DocumentZonalVerificationStatus;
import com.bob.db.enums.UserRole;
import com.bob.db.mapper.CandidateApplicationDocumentVerificationMapper;
import com.bob.db.repository.CandidateApplicationsRepository;
import com.bob.db.repository.CandidateApplicationDocumentVerificationRepository;
import com.bob.db.repository.DocumentTypesRepository;
import com.bob.db.repository.UserRepository;
import com.bob.db.util.DBConstants;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.entity.CandidateDocumentStoreEntity;
import com.bob.db.mapper.CandidateDocumentStoreMapper;
import com.bob.db.repository.CandidateDocumentStoreRepository;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.jobportal.model.AddAdditionalRequiredDocumentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class DocumentVerificationService {

    @Autowired
    private CandidateDocumentStoreRepository candidateDocumentStoreRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private CandidateDocumentStoreMapper candidateDocumentStoreMapper ;

    @Autowired
    private CandidateApplicationDocumentVerificationRepository applicationDocumentVerificationRepository;

    @Autowired
    private CandidateApplicationDocumentVerificationMapper applicationDocumentVerificationMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    public CandidateApplicationDocumentVerificationDTO saveDocumentScreeningDetails(CandidateApplicationDocumentVerificationDTO candidateDocumentStoreDTO){


        UUID currentUserId = securityUtils.getCurrentUserId();
        
        // Get current user to check role
        UserEntity currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CandidateApplicationDocumentVerificationEntity documentVerificationEntity;
        if (candidateDocumentStoreDTO.getId() != null) {
            documentVerificationEntity = applicationDocumentVerificationRepository
                    .findById(candidateDocumentStoreDTO.getId())
                    .orElse(new CandidateApplicationDocumentVerificationEntity());
        } else if (candidateDocumentStoreDTO.getCandidateDocumentId() != null) {
            documentVerificationEntity = applicationDocumentVerificationRepository
                    .findByApplicationIdAndCandidateDocumentId(candidateDocumentStoreDTO.getApplicationId(),candidateDocumentStoreDTO.getCandidateDocumentId())
                    .or(() -> applicationDocumentVerificationRepository.findById(candidateDocumentStoreDTO.getCandidateDocumentId()))
                    .orElse(new CandidateApplicationDocumentVerificationEntity());
        } else {
            throw new IllegalArgumentException("verificationId is required when candidateDocumentId is null");
        }

        UUID existingCandidateDocumentId = documentVerificationEntity.getCandidateDocumentId();
        boolean preserveNullCandidateDocumentId = existingCandidateDocumentId == null;

        // Check if user is Zonal HR using enum
        if (UserRole.ZONAL_HR.getValue().equalsIgnoreCase(currentUser.getRole())) {
            // Zonal HR verification logic - update zonal HR specific columns
            log.info("Zonal HR updating document verification for candidateId: {}, applicationId: {}, documentId: {}", 
                    candidateDocumentStoreDTO.getCandidateId(), 
                    candidateDocumentStoreDTO.getApplicationId(), 
                    candidateDocumentStoreDTO.getCandidateDocumentId());
            
            documentVerificationEntity.setZonalHrDocStatus(candidateDocumentStoreDTO.getZonalHrDocStatus());
            documentVerificationEntity.setZonalHrDocComments(candidateDocumentStoreDTO.getZonalHrDocComments());
            
            // Keep other fields as is - don't update from DTO to avoid overwriting screening data
            if (documentVerificationEntity.getApplicationId() == null) {
                documentVerificationEntity.setApplicationId(candidateDocumentStoreDTO.getApplicationId());
            }
            if (documentVerificationEntity.getCandidateId() == null) {
                documentVerificationEntity.setCandidateId(candidateDocumentStoreDTO.getCandidateId());
            }
            if (documentVerificationEntity.getCandidateDocumentId() == null && !preserveNullCandidateDocumentId) {
                documentVerificationEntity.setCandidateDocumentId(candidateDocumentStoreDTO.getCandidateDocumentId());
            }
        } else {
            // Regular screening logic - existing behavior for other users
            log.info("Regular screening user updating document verification for applicationId: {}, documentId: {}", 
                    candidateDocumentStoreDTO.getApplicationId(), 
                    candidateDocumentStoreDTO.getCandidateDocumentId());
            
            applicationDocumentVerificationMapper.updateEntityFromDto(candidateDocumentStoreDTO, documentVerificationEntity);
            documentVerificationEntity.setLastScreenedByUserId(currentUserId);
        }

        // Additional required docs intentionally keep candidate_document_id as null in DB.
        if (preserveNullCandidateDocumentId) {
            documentVerificationEntity.setCandidateDocumentId(null);
        }
        
        return enrichAdditionalDocIdentifiers(
                applicationDocumentVerificationMapper.toDto(applicationDocumentVerificationRepository.save(documentVerificationEntity))
        );
    }

    public CandidateApplicationDocumentVerificationDTO addAdditionalRequiredDocument(AddAdditionalRequiredDocumentRequest request) {
        UUID currentUserId = securityUtils.getCurrentUserId();

        CandidateApplicationsEntity application = candidateApplicationsRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!application.getCandidateId().equals(request.getCandidateId())) {
            throw new IllegalArgumentException("Candidate does not belong to the provided application");
        }

        DocumentTypesEntity additionalDocType = documentTypesRepository.findByDocCode("ADDRD");
        if (additionalDocType == null) {
            throw new ResourceNotFoundException("Document type with code ADDRD not found");
        }

        CandidateApplicationDocumentVerificationEntity newAdditionalDoc = CandidateApplicationDocumentVerificationEntity.builder()
                .candidateDocumentId(null)
                .candidateId(request.getCandidateId())
                .applicationId(request.getApplicationId())
                .docScreeningStatus(DocumentScreeningStatus.REJECTED)
                .docScreeningComments(null)
                .lastScreenedByUserId(currentUserId)
                .zonalHrDocStatus(DocumentZonalVerificationStatus.PENDING)
                .zonalHrDocComments(null)
                .documentId(additionalDocType.getId())
                .fileUrl(null)
                .displayName(request.getDisplayName().trim())
                .isValidationPending(Boolean.FALSE)
                .documentNumber(null)
                .pendingChecks(null)
                .build();

        return enrichAdditionalDocIdentifiers(
                applicationDocumentVerificationMapper.toDto(applicationDocumentVerificationRepository.save(newAdditionalDoc))
        );
    }

    public void deleteAdditionalRequiredDocument(UUID verificationId) {
        CandidateApplicationDocumentVerificationEntity docVerificationEntity = applicationDocumentVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Document verification entry not found"));

        if (docVerificationEntity.getDocumentId() == null) {
            throw new IllegalArgumentException("Only additional required documents can be deleted");
        }

        DocumentTypesEntity docType = documentTypesRepository.findById(docVerificationEntity.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Document type not found"));

        if (!"ADDRD".equalsIgnoreCase(docType.getDocCode())) {
            throw new IllegalArgumentException("Only additional required documents can be deleted");
        }

        applicationDocumentVerificationRepository.delete(docVerificationEntity);
    }

    public List<CandidateApplicationDocumentVerificationDTO> getDocumentScreeningDetails(UUID applicationId) {
        List<CandidateApplicationDocumentVerificationEntity> documentScreeningDetails = applicationDocumentVerificationRepository.findByApplicationIdOrderByDisplayNameAsc(applicationId);
        return applicationDocumentVerificationMapper.toDtoList(documentScreeningDetails).stream()
                .map(this::enrichAdditionalDocIdentifiers)
                .toList();
    }

    private CandidateApplicationDocumentVerificationDTO enrichAdditionalDocIdentifiers(CandidateApplicationDocumentVerificationDTO dto) {
        if (dto.getCandidateDocumentId() == null) {
            dto.setCandidateDocumentId(dto.getId());
        }
        return dto;
    }
}
