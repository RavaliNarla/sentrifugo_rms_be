package com.bob.candidateportal.service;

import com.bob.db.model.CandidateCertificationsResponseModel;
import com.bob.commonutil.util.CandidateValidationUtil;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.FileService;
import com.bob.db.dto.CandidateCertificationsDTO;
import com.bob.db.dto.CandidateDocumentStoreDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.DocumentCode;
import com.bob.db.mapper.CandidateCertificationsMapper;
import com.bob.db.mapper.CandidateDocumentStoreMapper;
import com.bob.db.repository.*;
import com.bob.db.util.DBConstants;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CandidateCertificationsService {

    @Autowired
    private CandidateCertificationsRepository candidateCertificationsRepository;

    @Autowired
    private CandidateCertificationsMapper candidateCertificationsMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    private CandidatesRepository candidatesRepository;

    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    @Value("${candidate.document.upload.path}")
    private String CANDIDATE_DOCUMENT_FOLDER;

    @Value("${resume.http.url}")
    private String CANDIDATE_DOCUMENT_BASE_URL;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private CandidateDocumentStoreRepository candidateDocumentStoreRepository;

    @Autowired
    private CandidateDocumentStoreMapper candidateDocumentStoreMapper;

    @Autowired
    private CertificationMasterRepository certificationMasterRepository;

    @Autowired
    private CandidateValidationUtil candidateValidationUtil;

    @Transactional
    public CandidateCertificationsResponseModel saveCandidateCertifications(
            UUID candidateId,
            CandidateCertificationsDTO certificationsDTO,
            MultipartFile file
    ) throws IOException {

        candidateValidationUtil.validateCandidateExistence(candidateId);

        if (certificationsDTO == null) {
            throw new IllegalArgumentException("Certification details cannot be null");
        }

        if(certificationsDTO.getCertificationId() ==null ){
            throw new IllegalArgumentException("Certification ID cannot be null or empty");
        }

        CertificationMasterEntity certificationMasterEntity = certificationMasterRepository.findById(certificationsDTO.getCertificationId())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid Certification ID"));

        CertificationMasterEntity otherCertification = certificationMasterRepository.findByCertificationNameIgnoreCase(DBConstants.OTHER);
        DocumentTypesEntity documentTypes =
                documentTypesRepository.findByDocCode(
                        DocumentCode.CERT.toString()
                );

        if (documentTypes == null) {
            throw new ResourceNotFoundException("Document type CERT not configured");
        }

        CandidateCertificationsEntity entity;

        if (certificationsDTO.getId() != null) {
            entity = candidateCertificationsRepository.findById(certificationsDTO.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Certification not found for id: " + certificationsDTO.getId()));

            candidateCertificationsMapper.updateEntityFromDto(certificationsDTO, entity);

        } else {
            entity = new CandidateCertificationsEntity();
            entity.setCandidateId(candidateId);

            candidateCertificationsMapper.updateEntityFromDto(certificationsDTO, entity);
        }

        if (certificationsDTO.getCertificationId().equals(otherCertification.getId())){
            entity.setCertificationName(certificationsDTO.getCertificationName());
        } else {
            entity.setCertificationName(certificationMasterEntity.getCertificationName());
        }

        CandidateCertificationsEntity savedEntity = candidateCertificationsRepository.save(entity);

        CandidateDocumentStoreEntity documentStore = null;
        if(certificationsDTO.getId() != null){
            documentStore = candidateDocumentStoreRepository.findByCandidateIdAndDocumentIdentifier(candidateId,certificationsDTO.getId()).orElse(null);
        }

        // Handle file upload
        if (file != null) {

            String fileName =
                    documentTypes.getDocumentName() + "_"
                            + savedEntity.getCertificationName() + "_"
                            + candidateId + "_"
                            + savedEntity.getId();

            String path = fileService.uploadFile(
                    file, fileName, CANDIDATE_DOCUMENT_FOLDER);
            
            // For Azure: CANDIDATE_DOCUMENT_BASE_URL is empty, use blob path
            // For E2E: CANDIDATE_DOCUMENT_BASE_URL has domain URL
            String fileUrl = (CANDIDATE_DOCUMENT_BASE_URL != null && !CANDIDATE_DOCUMENT_BASE_URL.isEmpty()) 
                ? CANDIDATE_DOCUMENT_BASE_URL + "/" + path 
                : CANDIDATE_DOCUMENT_FOLDER + "/" + path;

            CandidateDocumentStoreEntity.CandidateDocumentStoreEntityBuilder documentStoreBuilder =
                    CandidateDocumentStoreEntity.builder()
                            .candidateId(candidateId)
                            .documentId(documentTypes.getId())
                            .fileName(path)
                            .displayName(
                                    documentTypes.getDocumentName() + "_"
                                            + savedEntity.getCertificationName()
                            )
                            .fileUrl(fileUrl)
                            .documentIdentifier(savedEntity.getId());
            
            if (certificationsDTO.getIsValidationPending() != null) {
                documentStoreBuilder.isValidationPending(certificationsDTO.getIsValidationPending());
            }
            
            if (certificationsDTO.getPendingChecks() != null && !certificationsDTO.getPendingChecks().isEmpty()) {
                documentStoreBuilder.pendingChecks(certificationsDTO.getPendingChecks());
            }
            
            CandidateDocumentStoreEntity documentStoreEntity = documentStoreBuilder.build();

            if (documentStore != null) {
                documentStoreEntity.setId(documentStore.getId());
            }

            documentStore = candidateDocumentStoreRepository.save(documentStoreEntity);
        }

        return CandidateCertificationsResponseModel.builder()
                .certifications(candidateCertificationsMapper.toDto(savedEntity))
                .documentStore(
                        documentStore != null
                                ? candidateDocumentStoreMapper.toDTO(documentStore)
                                : null
                )
                .build();
    }

    public List<CandidateCertificationsResponseModel> getCandidateCertifications(UUID candidateId) {
        candidateValidationUtil.validateCandidateExistence(candidateId);

        List<CandidateCertificationsEntity> certificationEntities =   candidateCertificationsRepository.findByCandidateId(candidateId);

        List<UUID> certificationIds = certificationEntities.stream().map(CandidateCertificationsEntity::getId).toList();

        List<CandidateDocumentStoreEntity> candidateDocuments =  candidateDocumentStoreRepository.findByCandidateIdAndDocumentIdentifierIn(candidateId, certificationIds);

        Map<UUID, CandidateDocumentStoreDTO> documentMap =
                candidateDocuments.stream()
                        .collect(Collectors.toMap(
                                CandidateDocumentStoreEntity::getDocumentIdentifier,
                                candidateDocumentStoreMapper::toDTO,
                                (existing, replacement) -> existing
                        ));

        return certificationEntities.stream()
                .map(entity ->
                        CandidateCertificationsResponseModel.builder()
                                .certifications(candidateCertificationsMapper.toDto(entity))
                                .documentStore(documentMap.get(entity.getId()))
                                .build()
                )
                .toList();
    }

    @Transactional
    public void deleteCandidateCertification(UUID certificationId) {

        if (certificationId == null) {
            throw new IllegalArgumentException("Select a certificate");
        }
        Optional<CandidateCertificationsEntity> optional =candidateCertificationsRepository.findById(certificationId);

        if (optional.isPresent()) {
            candidateCertificationsRepository.deleteById(certificationId);
            List<CandidateDocumentStoreEntity> documents = candidateDocumentStoreRepository.findByDocumentIdentifier(certificationId);
            candidateDocumentStoreRepository.deleteAll(documents);
        } else {
            throw new ResourceNotFoundException("Certification record not found");
        }
    }


    @Transactional
    public Boolean saveHasCertification(UUID candidateId, Boolean hasCertification) {
        candidateValidationUtil.validateCandidateExistence(candidateId);
        CandidatesEntity candidatesEntity = candidatesRepository.findById(candidateId).orElseThrow(() -> new ResourceNotFoundException("Candidate not found."));

        if(!candidatesEntity.getIsProfileCompleted()){
            candidatesEntity.setCurrentStep(Short.parseShort("7"));
            candidatesRepository.save(candidatesEntity);
        }

        CandidateProfileEntity entity = candidateProfileRepository.findByCandidateId(candidateId).orElseThrow(()->new ResourceNotFoundException("Candidate profile not found."));
        entity.setHasCertification(hasCertification);

        CandidateProfileEntity savedEntity = candidateProfileRepository.save(entity);
        return savedEntity.getHasCertification();
    }

    public Boolean getHasCertification(UUID candidateId) {
        candidateValidationUtil.validateCandidateExistence(candidateId);
        CandidateProfileEntity entity = candidateProfileRepository.findByCandidateId(candidateId).orElseThrow(()->new ResourceNotFoundException("Candidate profile not found."));
        return entity.getHasCertification();
    }
}
