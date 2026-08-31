package com.bob.candidateportal.service;

import com.bob.commonutil.util.CandidateValidationUtil;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.model.candidateportal.ResumeModel;
import com.bob.commonutil.service.FileService;
import com.bob.commonutil.service.ResumeParserService;
import com.bob.db.entity.CandidateDocumentStoreEntity;
import com.bob.db.entity.CandidatesEntity;
import com.bob.db.entity.DocumentTypesEntity;
import com.bob.db.enums.DocumentCode;
import com.bob.db.repository.CandidateDocumentStoreRepository;
import com.bob.db.repository.CandidatesRepository;
import com.bob.db.repository.DocumentTypesRepository;
import com.bob.db.util.DBConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class CandidateResumeUploadService {

    @Autowired
    private CandidatesRepository candidatesRepository;

    @Autowired
    private FileService fileService;

    @Value("${candidate.document.upload.path}")
    private String CANDIDATE_DOCUMENT_FOLDER;

    @Value("${resume.http.url}")
    private String CANDIDATE_DOCUMENT_BASE_URL;

    @Autowired
    private ResumeParserService resumeParserService;

    @Autowired
    private CandidateDocumentStoreRepository candidateDocumentStoreRepository;

    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    @Autowired
    private CandidateValidationUtil candidateValidationUtil;


    public ResumeModel uploadResume( UUID candidateId,MultipartFile resume) throws IOException {
        if(resume == null || resume.isEmpty()) {
            log.error("No resume file provided for candidateId: {}", candidateId);
            throw new IllegalArgumentException("Resume file is required");
        }
        candidateValidationUtil.validateCandidateExistence(candidateId);
        DocumentTypesEntity documentTypes = documentTypesRepository.findByDocCode(DocumentCode.RESUME.toString());
        if (documentTypes == null) throw  new ResourceNotFoundException("Document type not found.");

        Optional<CandidateDocumentStoreEntity> existingResume = candidateDocumentStoreRepository
                .findByCandidateIdAndDocumentId(candidateId, documentTypes.getId());

        String fileName = DBConstants.CANDIDATE_RESUME +"_"+candidateId +"_"+documentTypes.getId();
        String path = fileService.uploadFile(resume, fileName, CANDIDATE_DOCUMENT_FOLDER);
        
        // For Azure: CANDIDATE_DOCUMENT_BASE_URL is empty, use blob path (CANDIDATE_DOCUMENT_FOLDER + path)
        // For E2E: CANDIDATE_DOCUMENT_BASE_URL has domain URL, use it
        String fileUrl = (CANDIDATE_DOCUMENT_BASE_URL != null && !CANDIDATE_DOCUMENT_BASE_URL.isEmpty()) 
            ? CANDIDATE_DOCUMENT_BASE_URL + "/" + path 
            : CANDIDATE_DOCUMENT_FOLDER + "/" + path;
        
        if (existingResume.isPresent()) {
            CandidateDocumentStoreEntity resumeEntity = existingResume.get();
            resumeEntity.setFileName(path);
            resumeEntity.setFileUrl(fileUrl);
            candidateDocumentStoreRepository.save(resumeEntity);
        } else {
            CandidateDocumentStoreEntity documentsEntity =
                    CandidateDocumentStoreEntity.builder()
                            .documentId(documentTypes.getId())
                            .displayName(documentTypes.getDocumentName())
                            .candidateId(candidateId)
                            .fileName(fileName)
                            .fileUrl(fileUrl)
                            .build();

            CandidatesEntity candidateEntity = candidatesRepository.findById(candidateId).orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
            candidateEntity.setCurrentStep(Short.parseShort("3"));
            candidateDocumentStoreRepository.save(documentsEntity);
        }

        ResumeModel resumeModel = resumeParserService.processResumeFileLLM(candidateId, CANDIDATE_DOCUMENT_FOLDER+"/"+path);
        return resumeModel;
    }

    public CandidateDocumentStoreEntity getResumeDetaile(UUID candidateId) {

        DocumentTypesEntity documentTypes = documentTypesRepository.findByDocumentName(DocumentCode.RESUME.toString());
        if(documentTypes==null) throw  new ResourceNotFoundException("Document type not found.");

        CandidateDocumentStoreEntity resumeEntity = candidateDocumentStoreRepository
                .findByCandidateIdAndDocumentId(candidateId, documentTypes.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found for candidate."));
        return resumeEntity;
    }
}
