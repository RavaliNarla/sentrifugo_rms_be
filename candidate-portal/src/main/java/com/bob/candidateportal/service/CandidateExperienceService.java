package com.bob.candidateportal.service;

import com.bob.commonutil.util.CandidateValidationUtil;
import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.model.WorkExperienceResponseModel;
import com.bob.commonutil.service.CandidateCommonGetService;
import com.bob.commonutil.service.FileService;
import com.bob.db.dto.WorkExperienceDTO;
import com.bob.db.entity.CandidateDocumentStoreEntity;
import com.bob.db.entity.CandidatesEntity;
import com.bob.db.entity.DocumentTypesEntity;
import com.bob.db.entity.WorkExperienceEntity;
import com.bob.db.enums.DocumentCode;
import com.bob.db.mapper.CandidateDocumentStoreMapper;
import com.bob.db.mapper.WorkExperienceMapper;
import com.bob.db.repository.CandidateDocumentStoreRepository;
import com.bob.db.repository.CandidatesRepository;
import com.bob.db.repository.DocumentTypesRepository;
import com.bob.db.repository.WorkExperienceRepository;
import com.bob.db.util.DBConstants;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class CandidateExperienceService {
    @Autowired
    private CandidatesRepository candidatesRepository;

    @Autowired
    private WorkExperienceRepository workExperienceRepository;

    @Autowired
    private WorkExperienceMapper workExperienceMapper;

    @Autowired
    private FileService fileService;

    @Value("${candidate.document.upload.path}")
    private String CANDIDATE_DOCUMENT_FOLDER;

    @Value("${resume.http.url}")
    private String CANDIDATE_DOCUMENT_BASE_URL;

    @Autowired
    private CandidateDocumentStoreRepository candidateDocumentStoreRepository;

    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    @Autowired
    private CandidateDocumentStoreMapper candidateDocumentStoreMapper;



    @Autowired
    private CandidateCommonGetService candidateCommonGetService;

    @Autowired
    private CandidateValidationUtil candidateValidationUtil;


    @Transactional
    public WorkExperienceResponseModel saveExperienceDetails(UUID candidateId, WorkExperienceDTO workExperience, MultipartFile file) {
        candidateValidationUtil.validateCandidateExistence(candidateId);
        if(workExperience == null) {
            throw new IllegalArgumentException("Work experience details cannot be null");
        }
        WorkExperienceEntity entity;

        DocumentTypesEntity documentTypes = documentTypesRepository.findByDocCode(DocumentCode.WORKEX.toString());

        if(documentTypes==null){
            throw new IllegalStateException("Document type RESUME not configured in document_types table");
        }

        CandidateDocumentStoreEntity documentStore = null;
        if(workExperience.getId() != null) {
            entity = workExperienceRepository.findById(workExperience.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Work experience record not found"));
            workExperienceMapper.updateEntityFromDto(workExperience, entity);
        } else {

            CandidatesEntity candidateEntity = candidatesRepository.findById(candidateId).orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
            candidateEntity.setCurrentStep(Short.parseShort("8"));
            entity = workExperienceMapper.toEntity(workExperience);
            entity.setCandidateId(candidateId);
        }
        WorkExperienceEntity savedEntity = workExperienceRepository.save(entity);

        if(workExperience.getId() != null){
            documentStore = candidateDocumentStoreRepository.findByCandidateIdAndDocumentIdentifier(candidateId,workExperience.getId()).orElse(null);
        }


        if(file != null){
            String fileName = DBConstants.WORK_EXPERIENCE + candidateId + "_" + savedEntity.getId();
            try {
                String path = fileService.uploadFile(file, fileName, CANDIDATE_DOCUMENT_FOLDER);
                
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
                        .displayName(documentTypes.getDocumentName()+"_"+savedEntity.getOrganizationName())
                        .fileUrl(fileUrl)
                        .documentIdentifier(savedEntity.getId());
                
                if (workExperience.getIsValidationPending() != null) {
                    documentStoreBuilder.isValidationPending(workExperience.getIsValidationPending());
                }
                
                if (workExperience.getPendingChecks() != null && !workExperience.getPendingChecks().isEmpty()) {
                    documentStoreBuilder.pendingChecks(workExperience.getPendingChecks());
                }
                
                CandidateDocumentStoreEntity documentStoreEntity = documentStoreBuilder.build();
                if(workExperience.getId() != null) {
                    //Updating existing document record if updating
                   if (documentStore != null ){
                       documentStoreEntity.setId(documentStore.getId());
                   }
                }
                documentStore = candidateDocumentStoreRepository.save(documentStoreEntity);
            } catch (Exception e) {
                log.error("Error uploading work experience document for candidate {}: {}", candidateId, e.getMessage());
                throw new CommonException("Failed to upload work experience document");
            }
        }
        return WorkExperienceResponseModel.builder().workExperience(workExperienceMapper.toDTO(savedEntity))
                .documentStore(candidateDocumentStoreMapper.toDTO(documentStore)).build();

    }

    public List<WorkExperienceResponseModel> getCandidateExperienceDetails(UUID candidateId) {
       return candidateCommonGetService.getCandidateExperienceDetails(candidateId);
    }

    public void deleteCandidateExperience(UUID workExperienceId) {
        if(workExperienceId == null) {
            throw new IllegalArgumentException("Experience ID cannot be null");
        }
        Optional<WorkExperienceEntity> optional = workExperienceRepository.findById(workExperienceId);
        if(optional.isPresent()) {
            workExperienceRepository.deleteById(workExperienceId);
            // Also delete associated document if exists
            List<CandidateDocumentStoreEntity> documents = candidateDocumentStoreRepository.findByDocumentIdentifier(workExperienceId) ;
            candidateDocumentStoreRepository.deleteAll(documents);
        } else {
            throw new ResourceNotFoundException("Work experience record not found.");
        }
    }



    record DateRange(LocalDate start, LocalDate end) {}
    @Transactional
    public List<WorkExperienceDTO> saveAllExperienceDetails(
            UUID candidateId,
            List<WorkExperienceDTO> incomingList) {

        candidateValidationUtil.validateCandidateExistence(candidateId);

        if (incomingList == null || incomingList.isEmpty()) {
            throw new IllegalArgumentException("Work experience list cannot be empty");
        }

        LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

        // 🔹 Single DB call
        List<WorkExperienceEntity> existing =
                workExperienceRepository.findByCandidateId(candidateId);

        // 🔹 Existing ranges
        List<DateRange> acceptedRanges = new ArrayList<>();

        for (WorkExperienceEntity e : existing) {
            acceptedRanges.add(new DateRange(
                    e.getFromDate(),
                    e.getToDate() == null ? MAX_DATE : e.getToDate()
            ));
        }

        // 🔹 Sort existing ranges
        acceptedRanges.sort(Comparator.comparing(DateRange::start));

        List<WorkExperienceDTO> validToSave = new ArrayList<>();

        // 🔹 Process incoming records
        for (WorkExperienceDTO dto : incomingList) {

            if (dto.getFromDate() == null) {
                continue; // ignore invalid record
            }

            LocalDate toDate = Boolean.TRUE.equals(dto.getIsPresentlyWorking())
                    ? MAX_DATE
                    : dto.getToDate();

            if (toDate != null && dto.getFromDate().isAfter(toDate)) {
                continue; // ignore invalid record
            }

            DateRange incomingRange =
                    new DateRange(dto.getFromDate(), toDate);

            boolean overlaps = acceptedRanges.stream()
                    .anyMatch(existingRange ->
                            isOverlapping(existingRange, incomingRange));

            if (overlaps) {
                continue; // 🚫 IGNORE overlapping record
            }

            // ✅ Accept
            acceptedRanges.add(incomingRange);
            acceptedRanges.sort(Comparator.comparing(DateRange::start));

            dto.setCandidateId(candidateId);
            validToSave.add(dto);
        }

        // 🔹 Save only non-overlapping records
        if (validToSave.isEmpty()) {
            return List.of(); // nothing to save
        }

        List<WorkExperienceEntity> entities =
                workExperienceMapper.toEntityList(validToSave);

        return workExperienceMapper.toDTOList(
                workExperienceRepository.saveAll(entities)
        );
    }

    private boolean isOverlapping(DateRange a, DateRange b) {
        return !a.end().isBefore(b.start()) &&
                !b.end().isBefore(a.start());
    }

}
