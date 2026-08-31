package com.bob.candidateportal.service;

import com.bob.commonutil.util.CandidateValidationUtil;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.model.EducationResponseModel;
import com.bob.commonutil.service.CandidateCommonGetService;
import com.bob.commonutil.service.FileService;
import com.bob.db.dto.EducationDTO;
import com.bob.db.entity.*;
import com.bob.db.mapper.CandidateDocumentStoreMapper;
import com.bob.db.mapper.EducationMapper;
import com.bob.db.repository.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class CandidateEducationService {
    @Autowired
    private CandidatesRepository candidatesRepository;


    @Autowired
    private EducationRepository educationRepository;

    @Autowired
    private EducationMapper educationMapper;

    @Autowired
    private CandidateDocumentStoreRepository candidateDocumentStoreRepository;

    @Value("${candidate.document.upload.path}")
    private String CANDIDATE_DOCUMENT_FOLDER;

    @Value("${resume.http.url}")
    private String CANDIDATE_DOCUMENT_BASE_URL;

    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    @Autowired
    private CandidateDocumentStoreMapper candidateDocumentStoreMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    private EducationQualificationsRepository educationQualificationsRepository;

    @Autowired
    private CandidateCommonGetService candidateCommonGetService;

    @Autowired
    private CandidateValidationUtil candidateValidationUtil;


    @Transactional
    public EducationResponseModel saveCandidateEducation(
            UUID candidateId,
            EducationDTO education,
            MultipartFile file,
            String docCode
    ) throws IOException {
       candidateValidationUtil.validateCandidateExistence(candidateId);

        if (education.getEducationQualificationsId() == null) {
            throw new IllegalArgumentException("Education Qualification is mandatory");
        }

        EducationQualificationsEntity qualification =
                educationQualificationsRepository.findById(
                        education.getEducationQualificationsId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Education Qualification not found."));

        DocumentTypesEntity documentType = documentTypesRepository.findByDocCode(docCode);
        if (documentType == null) {
            throw new ResourceNotFoundException("Document Type not found for code: " + docCode);
        }


        EducationEntity entity;

        if (education.getId() != null) {
            entity = educationRepository.findById(education.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Education record not found."));

            educationMapper.updateEntityFromDto(education, entity);
        } else {

            CandidatesEntity candidateEntity = candidatesRepository.findById(candidateId).orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
            candidateEntity.setCurrentStep(Short.parseShort("6"));
            entity = educationMapper.toEntity(education);
            entity.setCandidateId(candidateId);
        }

        if (entity.getIsSubmitted() == null) {
            entity.setIsSubmitted(false);
        }

        EducationEntity savedEntity = educationRepository.save(entity);

        CandidateDocumentStoreEntity documentStore = null;

        if (file != null && !file.isEmpty()) {

            documentStore = candidateDocumentStoreRepository
                    .findByCandidateIdAndDocumentIdentifier(candidateId, savedEntity.getId())
                    .orElse(null);

            String fileName = documentType.getDocumentName()
                    + "_" + candidateId
                    + "_" + savedEntity.getId();

            String path = fileService.uploadFile(
                    file, fileName, CANDIDATE_DOCUMENT_FOLDER
            );
            
            // For Azure: CANDIDATE_DOCUMENT_BASE_URL is empty, use blob path
            // For E2E: CANDIDATE_DOCUMENT_BASE_URL has domain URL
            String fileUrl = (CANDIDATE_DOCUMENT_BASE_URL != null && !CANDIDATE_DOCUMENT_BASE_URL.isEmpty()) 
                ? CANDIDATE_DOCUMENT_BASE_URL + "/" + path 
                : CANDIDATE_DOCUMENT_FOLDER + "/" + path;

            CandidateDocumentStoreEntity.CandidateDocumentStoreEntityBuilder documentStoreBuilder =
                    CandidateDocumentStoreEntity.builder()
                            .candidateId(candidateId)
                            .documentId(documentType.getId())
                            .fileName(fileName)
                            .displayName(documentType.getDocumentName())
                            .fileUrl(fileUrl)
                            .documentIdentifier(savedEntity.getId());
            
            if (education.getIsValidationPending() != null) {
                documentStoreBuilder.isValidationPending(education.getIsValidationPending());
            }
            
            if (education.getPendingChecks() != null && !education.getPendingChecks().isEmpty()) {
                documentStoreBuilder.pendingChecks(education.getPendingChecks());
            }
            
            CandidateDocumentStoreEntity documentStoreEntity = documentStoreBuilder.build();

            if (documentStore != null) {
                documentStoreEntity.setId(documentStore.getId());
            }

            documentStore = candidateDocumentStoreRepository.save(documentStoreEntity);
        }

        return EducationResponseModel.builder()
                .education(educationMapper.toDTO(savedEntity))
                .documentStore(
                        documentStore != null
                                ? candidateDocumentStoreMapper.toDTO(documentStore)
                                : null
                )
                .build();
    }


    public List<EducationResponseModel > getCandidateEducationDetails(UUID candidateId) {
       return candidateCommonGetService.getCandidateEducationDetails(candidateId);
    }

    public void deleteCandidateEducationDetails(UUID educationId) {
        if(educationId == null) {
            throw new IllegalArgumentException("Select a education Qualification.");
        }
        Optional<EducationEntity> optional = educationRepository.findById(educationId);
        if(optional.isPresent()) {
            educationRepository.deleteById(educationId);
            // Also delete associated document if exists
            List<CandidateDocumentStoreEntity> documents = candidateDocumentStoreRepository.findByDocumentIdentifier(educationId) ;
            candidateDocumentStoreRepository.deleteAll(documents);
        } else {
            throw new ResourceNotFoundException("Education record not found.");
        }
    }
}
