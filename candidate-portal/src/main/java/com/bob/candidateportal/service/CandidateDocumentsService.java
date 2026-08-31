package com.bob.candidateportal.service;

import com.bob.commonutil.util.CandidateValidationUtil;
import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.CandidateCommonGetService;
import com.bob.commonutil.service.FileService;
import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.CandidateDocumentStoreDTO;
import com.bob.db.entity.CandidateDocumentStoreEntity;
import com.bob.db.entity.DocumentTypesEntity;
import com.bob.db.entity.EducationEntity;
import com.bob.db.enums.DocumentCode;
import com.bob.db.mapper.CandidateDocumentStoreMapper;
import com.bob.db.repository.CandidateDocumentStoreRepository;
import com.bob.db.repository.CandidatesRepository;
import com.bob.db.repository.DocumentTypesRepository;
import com.bob.db.repository.EducationRepository;
import com.bob.db.util.DBConstants;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class CandidateDocumentsService {
    @Autowired
    private CandidatesRepository candidatesRepository;

    @Autowired
    private FileService fileService;

    @Value("${candidate.document.upload.path}")
    private String CANDIDATE_DOCUMENT_FOLDER;

    @Value("${resume.http.url}")
    private String CANDIDATE_DOCUMENT_BASE_URL;

    @Autowired
    private CandidateDocumentStoreRepository candidateDocumentStoreRepository;

    @Autowired
    private CandidateDocumentStoreMapper candidateDocumentStoreMapper;

    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    @Autowired
    private CandidateCommonGetService candidateCommonGetService;

    @Autowired
    private FaceDetectionService faceDetectionService;

    @Autowired
    private CandidateValidationUtil candidateValidationUtil;


    @Autowired
    private EducationRepository educationRepository;
    @Transactional
    public CandidateDocumentStoreDTO uploadCandidateDocuments(UUID candidateId, UUID documentId, Boolean isValidationPending, MultipartFile documents) {
        candidateValidationUtil.validateCandidateExistence(candidateId);
        if (documents == null || documents.isEmpty()) {
            throw new IllegalArgumentException("No documents provided for upload");
        }
        DocumentTypesEntity docTypeOpt = documentTypesRepository.findById(documentId).orElseThrow(() -> new ResourceNotFoundException("Invalid document type ID"));

        String fileName = DBConstants.CANDIDATE + candidateId+ "_" +docTypeOpt.getDocumentName() ;
        try {
            String path = fileService.uploadFile(documents, fileName, CANDIDATE_DOCUMENT_FOLDER);
            
            // For Azure: CANDIDATE_DOCUMENT_BASE_URL is empty, use blob path
            // For E2E: CANDIDATE_DOCUMENT_BASE_URL has domain URL
            String fileUrl = (CANDIDATE_DOCUMENT_BASE_URL != null && !CANDIDATE_DOCUMENT_BASE_URL.isEmpty()) 
                ? CANDIDATE_DOCUMENT_BASE_URL + "/" + path 
                : CANDIDATE_DOCUMENT_FOLDER + "/" + path;
            
            CandidateDocumentStoreEntity.CandidateDocumentStoreEntityBuilder builder = CandidateDocumentStoreEntity.builder()
                    .candidateId(candidateId)
                    .documentId(documentId)
                    .displayName(docTypeOpt.getDocumentName())
                    .fileName(fileName)
                    .fileUrl(fileUrl);
            
            if (isValidationPending != null) {
                builder.isValidationPending(isValidationPending);
            }
            
            CandidateDocumentStoreEntity documentsEntity = builder.build();
            return candidateDocumentStoreMapper.toDTO(candidateDocumentStoreRepository.save(documentsEntity));
        } catch (Exception e) {
            log.error("Error uploading document for candidateId {}: {}", candidateId, e.getMessage());
            throw new CommonException("Failed to upload document");
        }

    }

    @Transactional
    public CandidateDocumentStoreDTO uploadCandidateDocumentWithValidation(UUID candidateId, UUID documentId, 
                                                                            Boolean isValidationPending, 
                                                                            List<String> pendingChecks, 
                                                                            MultipartFile file) throws Exception {
        candidateValidationUtil.validateCandidateExistence(candidateId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No documents provided for upload");
        }
        DocumentTypesEntity docTypeOpt = documentTypesRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid document type."));

        //Run face detection model
        if(docTypeOpt.getDocCode().equals(AppConstants.PHOTO_DOC_CODE)){
            faceDetectionService.validatePassportPhoto(file);
        }

        String fileName = DBConstants.CANDIDATE + candidateId + "_" + docTypeOpt.getDocumentName();
        try {
            String path = fileService.uploadFile(file, fileName, CANDIDATE_DOCUMENT_FOLDER);

            // For Azure: CANDIDATE_DOCUMENT_BASE_URL is empty, use blob path
            // For E2E: CANDIDATE_DOCUMENT_BASE_URL has domain URL
            String fileUrl = (CANDIDATE_DOCUMENT_BASE_URL != null && !CANDIDATE_DOCUMENT_BASE_URL.isEmpty()) 
                ? CANDIDATE_DOCUMENT_BASE_URL + "/" + path 
                : CANDIDATE_DOCUMENT_FOLDER + "/" + path;
            
            CandidateDocumentStoreEntity.CandidateDocumentStoreEntityBuilder builder = CandidateDocumentStoreEntity.builder()
                    .candidateId(candidateId)
                    .documentId(documentId)
                    .displayName(docTypeOpt.getDocumentName())
                    .fileName(fileName)
                    .fileUrl(fileUrl);
            
            if (isValidationPending != null) {
                builder.isValidationPending(isValidationPending);
            }
            
            if (pendingChecks != null && !pendingChecks.isEmpty()) {
                builder.pendingChecks(pendingChecks);
            }
            
            CandidateDocumentStoreEntity documentsEntity = builder.build();
            return candidateDocumentStoreMapper.toDTO(candidateDocumentStoreRepository.save(documentsEntity));
        } catch (Exception e) {
            log.error("Error uploading document with validation data for candidateId {}: {}", candidateId, e.getMessage());
            throw new CommonException("Failed to upload document");
        }
    }

    public List<CandidateDocumentStoreDTO> getCandidateDocuments(UUID candidateId) {
        return candidateCommonGetService.getCandidateDocuments(candidateId);
    }

    public CandidateDocumentStoreDTO getCandidateDocumentsByDocCode(UUID candidateId,String docCode) {
        DocumentTypesEntity documentTypes = documentTypesRepository.findByDocCode(docCode);
        if (documentTypes == null) {
            throw new ResourceNotFoundException("Invalid document code: " + docCode);
        }
            CandidateDocumentStoreEntity documentEntity = candidateDocumentStoreRepository
                .findByCandidateIdAndDocumentId(candidateId, documentTypes.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No document found."));
        return candidateDocumentStoreMapper.toDTO(documentEntity);
    }

    public void deleteCandidateDocuments(UUID candidateId, UUID documentId) {
        CandidateDocumentStoreEntity documentEntity = candidateDocumentStoreRepository
                .findByCandidateIdAndDocumentId(candidateId, documentId)
                .orElseThrow(() -> new ResourceNotFoundException("No document found."));
        try {
            fileService.deleteExistingFiles(CANDIDATE_DOCUMENT_FOLDER, documentEntity.getFileName());
            candidateDocumentStoreRepository.delete(documentEntity);
        } catch (Exception e) {
            log.error("Error deleting document for candidateId {}: {}", candidateId, e.getMessage());
            throw new CommonException("Failed to delete document");
        }
    }

    public CandidateDocumentStoreDTO uploadOtherCandidateDocuments(UUID candidateId, String docName, MultipartFile documents) {
        candidateValidationUtil.validateCandidateExistence(candidateId);
        if (documents == null || documents.isEmpty()) {
            throw new IllegalArgumentException("No documents provided for upload");
        }
        DocumentTypesEntity docTypeOpt = documentTypesRepository.findByDocCode(DocumentCode.OTHERS.toString());
        if (docTypeOpt == null) {
            throw new ResourceNotFoundException("Document type 'Others' not found");
        }
        CandidateDocumentStoreEntity existingDoc = candidateDocumentStoreRepository
                .findByCandidateIdAndDocumentId(candidateId, docTypeOpt.getId())
                .orElse(null);
        if(existingDoc != null){
            try {
                fileService.deleteExistingFiles(CANDIDATE_DOCUMENT_FOLDER, existingDoc.getFileName());
                candidateDocumentStoreRepository.delete(existingDoc);
            } catch (Exception e) {
                log.error("Error deleting existing document for candidateId {}: {}", candidateId, e.getMessage());
                throw new CommonException("Failed to delete existing document");
            }
        }
        String fileName =  docTypeOpt.getDocumentName()+"_"+docName+"_"+candidateId;
        try {
            String path = fileService.uploadFile(documents, fileName, CANDIDATE_DOCUMENT_FOLDER);
            
            // For Azure: CANDIDATE_DOCUMENT_BASE_URL is empty, use blob path
            // For E2E: CANDIDATE_DOCUMENT_BASE_URL has domain URL
            String fileUrl = (CANDIDATE_DOCUMENT_BASE_URL != null && !CANDIDATE_DOCUMENT_BASE_URL.isEmpty()) 
                ? CANDIDATE_DOCUMENT_BASE_URL + "/" + path 
                : CANDIDATE_DOCUMENT_FOLDER + "/" + path;
            
            CandidateDocumentStoreEntity documentsEntity = CandidateDocumentStoreEntity.builder()
                    .candidateId(candidateId)
                    .documentId(docTypeOpt.getId())
                    .displayName(docName)
                    .fileName(fileName)
                    .fileUrl(fileUrl)
                    .build();
            return candidateDocumentStoreMapper.toDTO(candidateDocumentStoreRepository.save(documentsEntity));
        } catch (Exception e) {
            log.error("Error uploading document for candidateId {}: {}", candidateId, e.getMessage());
            throw new CommonException("Failed to upload document");
        }
    }

    @Transactional
    public CandidateDocumentStoreDTO uploadIdProofDocument(UUID candidateId, UUID documentId, String documentNumber, 
                                                           Boolean isValidationPending, List<String> pendingChecks, 
                                                           MultipartFile file, boolean isDigilocker) {
        candidateValidationUtil.validateCandidateExistence(candidateId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided for upload");
        }
        if (documentNumber == null || documentNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Document number is required");
        }
        
        // Trim all spaces from document number (frontend does this too, but safety measure)
        String normalizedDocNumber = documentNumber.replaceAll("\\s+", "");

        DocumentTypesEntity docTypeOpt = documentTypesRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found."));

        // Check if document with same documentId and documentNumber already exists for this candidate
        CandidateDocumentStoreEntity existingDoc = candidateDocumentStoreRepository
                .findByCandidateIdAndDocumentIdAndDocumentNumber(candidateId, documentId, normalizedDocNumber)
                .orElse(null);
        
        if (existingDoc != null) {
            // Delete existing document before uploading new one
            try {
                fileService.deleteExistingFiles(CANDIDATE_DOCUMENT_FOLDER, existingDoc.getFileName());
                candidateDocumentStoreRepository.delete(existingDoc);
            } catch (Exception e) {
                log.error("Error deleting existing ID proof document for candidateId {}: {}", candidateId, e.getMessage());
                throw new CommonException("Failed to delete existing ID proof document");
            }
        }

        String fileName = DBConstants.CANDIDATE + candidateId + "_" + docTypeOpt.getDocumentName();
        try {
            String path = fileService.uploadFile(file, fileName, CANDIDATE_DOCUMENT_FOLDER);

            // For Azure: CANDIDATE_DOCUMENT_BASE_URL is empty, use blob path
            // For E2E: CANDIDATE_DOCUMENT_BASE_URL has domain URL
            String fileUrl = (CANDIDATE_DOCUMENT_BASE_URL != null && !CANDIDATE_DOCUMENT_BASE_URL.isEmpty())
                    ? CANDIDATE_DOCUMENT_BASE_URL + "/" + path
                    : CANDIDATE_DOCUMENT_FOLDER + "/" + path;

            CandidateDocumentStoreEntity.CandidateDocumentStoreEntityBuilder builder = CandidateDocumentStoreEntity.builder()
                    .candidateId(candidateId)
                    .documentId(documentId)
                    .displayName(docTypeOpt.getDocumentName())
                    .fileName(fileName)
                    .fileUrl(fileUrl)
                    .documentNumber(normalizedDocNumber)
                    .isDigilocker(isDigilocker);
            
            if (isValidationPending != null) {
                builder.isValidationPending(isValidationPending);
            }
            
            if (pendingChecks != null && !pendingChecks.isEmpty()) {
                builder.pendingChecks(pendingChecks);
            }
            
            CandidateDocumentStoreEntity documentsEntity = builder.build();
            return candidateDocumentStoreMapper.toDTO(candidateDocumentStoreRepository.save(documentsEntity));
        } catch (Exception e) {
            log.error("Error uploading ID proof document for candidateId {}: {}", candidateId, e.getMessage());
            throw new CommonException("Failed to upload ID proof document");
        }
    }
    @Transactional
    public CandidateDocumentStoreDTO uploadDigilockerDocuments(UUID candidateId,UUID educationId, UUID documentId, MultipartFile documents) {
        candidateValidationUtil.validateCandidateExistence(candidateId);
        if (documents == null || documents.isEmpty()) {
            throw new IllegalArgumentException("No documents provided for upload");
        }
        DocumentTypesEntity docTypeOpt = documentTypesRepository.findById(documentId).orElseThrow(() -> new ResourceNotFoundException("Invalid document type ID"));

        Optional<CandidateDocumentStoreEntity> existingDocument = candidateDocumentStoreRepository.findByCandidateIdAndDocumentId(
                candidateId,
                documentId
        );

        if (existingDocument.isPresent()) {
            try {
                fileService.deleteExistingFiles(CANDIDATE_DOCUMENT_FOLDER, existingDocument.get().getFileName());
                candidateDocumentStoreRepository.delete(existingDocument.get());

            } catch (Exception e) {
                log.error("Error deleting existing DigiLocker document for candidateId {}: {}", candidateId, e.getMessage());
                throw new CommonException("Failed to delete existing DigiLocker document");
            }
        }

        String fileName = DBConstants.CANDIDATE + candidateId + "_" + docTypeOpt.getDocumentName();

        try {
            String path = fileService.uploadFile(documents, fileName, CANDIDATE_DOCUMENT_FOLDER);

            String fileUrl = (CANDIDATE_DOCUMENT_BASE_URL != null
                    && !CANDIDATE_DOCUMENT_BASE_URL.isEmpty())
                    ? CANDIDATE_DOCUMENT_BASE_URL + "/" + path
                    : CANDIDATE_DOCUMENT_FOLDER + "/" + path;

            CandidateDocumentStoreEntity documentsEntity =
                    CandidateDocumentStoreEntity.builder()
                            .candidateId(candidateId)
                            .documentId(documentId)
                            .displayName(docTypeOpt.getDocumentName())
                            .fileName(fileName)
                            .fileUrl(fileUrl)
                            .isDigilocker(true)
                            .documentIdentifier(educationId)
                            .build();

            log.info("Uploaded image");
            return candidateDocumentStoreMapper.toDTO(
                    candidateDocumentStoreRepository.save(documentsEntity)
            );

        } catch (Exception e) {
            log.error("Error uploading DigiLocker document for candidateId {}: {}", candidateId, e.getMessage());
            throw new CommonException("Failed to upload DigiLocker document");
        }
    }


}
