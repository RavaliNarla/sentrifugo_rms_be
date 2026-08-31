package com.bob.candidateportal.service;

import com.bob.candidateportal.model.UpdateOfferDecisionModel;
import com.bob.commonutil.service.EmbeddingBuilderService;
import com.bob.commonutil.service.VacancyStatisticsService;
import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.db.dto.CandidateApplicationsDTO;
import com.bob.db.dto.CandidateLocationPreferenceDTO;
import com.bob.db.dto.CandidateOffersDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.CandidateApplicationStatus;
import com.bob.db.enums.CandidateOfferStatus;
import com.bob.db.enums.DocumentScreeningStatus;
import com.bob.db.enums.DocumentZonalVerificationStatus;
import com.bob.db.mapper.*;
import com.bob.db.repository.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Slf4j
@Service
public class CandidateJobAppService {

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private VacancyStatisticsService vacancyStatisticsService;

    @Autowired
    private WorkflowApprovalEntityRepository workflowApprovalRepository;

    @Autowired
    private CandidateApplicationsMapper candidateApplicationMapper;

    @Autowired
    private CandidateDocumentsMapper mapper;

    @Autowired
    private CandidateDocumentStoreRepository candidateDocumentStoreRepository;

    @Autowired
    private CandidateApplicationDocumentVerificationRepository candidateApplicationDocumentVerificationRepository;

    @Autowired
    private CandidateOffersRepository candidateOffersRepository;

    @Autowired
    private CandidateOffersMapper  candidateOffersMapper;

    @Autowired
    private EmbeddingBuilderService embeddingBuilderService;

    @Autowired
    private CandidateLocationPreferencesRepository candidateLocationPreferencesRepository;

    @Autowired
    private CandidateLocationPreferenceMapper candidateLocationPreferenceMapper;

    @Autowired
    private CandidateConcessionsRepository candidateConcessionsRepository;

    @Autowired
    private AgeRelaxationApplicationRepository ageRelaxationApplicationRepository;

    @Autowired
    private PositionsRepository positionsRepository;

    
    private static final Logger logger = LoggerFactory.getLogger(CandidateJobAppService.class);



    @Transactional
    public CandidateApplicationsDTO applyForJob(CandidateLocationPreferenceDTO candidateLocationPreferenceDTO) {

        UUID candidateId=candidateLocationPreferenceDTO.getCandidateId();
        UUID positionId=candidateLocationPreferenceDTO.getPositionId();

        // Optional<CandidateApplicationsEntity> application = candidateApplicationsRepository.findByCandidateIdAndPositionId(candidateId, positionId);

        if (candidateApplicationsRepository.existsByCandidateIdAndPositionId(candidateId,positionId)) {
            throw new CommonException("Candidate already applied for this position.");
        }
        //Save preference first
        CandidateLocationPreferenceEntity entity = candidateLocationPreferenceMapper.toEntity(candidateLocationPreferenceDTO);
        candidateLocationPreferencesRepository.save(entity);

        CandidateApplicationsEntity candidateApplication = CandidateApplicationsEntity.builder()
                .candidateId(candidateId)
                .positionId(positionId)
                .applicationStatus(CandidateApplicationStatus.APPLIED)
                .applicationDate(LocalDateTime.now())
                .build();
        //Save candidate application with workflow
        CandidateApplicationsEntity savedApplication = candidateApplicationsRepository.saveWithWorkflow(candidateApplication);
        upsertAgeConcession(savedApplication, candidateLocationPreferenceDTO);

        // add all documents from candidate document store to candidate application documents - new start
        List<CandidateDocumentStoreEntity> candidateDocumentStoreEntity = candidateDocumentStoreRepository.findAllByCandidateId(candidateId);
        List<CandidateApplicationDocumentVerificationEntity> applicationDocumentsEntities = candidateDocumentStoreEntity.stream()
                .map(doc -> CandidateApplicationDocumentVerificationEntity.builder()
                        .fileUrl(doc.getFileUrl())
                        .displayName(doc.getDisplayName())
                        .applicationId(savedApplication.getId())
                        .docScreeningStatus(DocumentScreeningStatus.PENDING)
                        .zonalHrDocStatus(DocumentZonalVerificationStatus.PENDING)
                        .documentId(doc.getDocumentId())
                        .candidateDocumentId(doc.getId())
                        .candidateId(candidateId)
                        .isValidationPending(doc.getIsValidationPending())
                        .documentNumber(doc.getDocumentNumber())
                        .pendingChecks(doc.getPendingChecks())
                        .isDigilocker(doc.isDigilocker())
                        .build())
                .toList();
        candidateApplicationDocumentVerificationRepository.saveAll(applicationDocumentsEntities);
        // add all documents from candidate document store to candidate application documents - new end

        CandidateApplicationsDTO savedDTO =candidateApplicationMapper.toDTO(savedApplication);
        try{
            embeddingBuilderService.generateEmbeddingForCandidate(savedDTO.getCandidateId(),savedDTO.getId());

        }catch (Exception e){
            log.error("Error generating embedding for candidate application. candidateId: {}, applicationId: {}, error: {}", savedDTO.getCandidateId(), savedDTO.getId(), e.getMessage());
        }
        return savedDTO;

    }

    private void upsertAgeConcession(CandidateApplicationsEntity savedApplication,
                                     CandidateLocationPreferenceDTO candidateLocationPreferenceDTO) {
        if (savedApplication == null || savedApplication.getId() == null) {
            return;
        }

        try {
            UUID candidateId = savedApplication.getCandidateId();
            UUID positionId = savedApplication.getPositionId();
            UUID selectedStateId = candidateLocationPreferenceDTO.getStatePreference1();
            UUID selectedCityId = resolveSelectedCityPreference(candidateLocationPreferenceDTO);

            boolean ageConcession = isAgeConcessionApplicable(candidateId, positionId, selectedStateId, selectedCityId);

            List<CandidateConcessionsEntity> existingConcessions =
                    candidateConcessionsRepository.findAllByApplicationIdIn(Collections.singletonList(savedApplication.getId()));

            if (!existingConcessions.isEmpty()) {
                CandidateConcessionsEntity existing = existingConcessions.get(0);
                existing.setAgeConcession(ageConcession);
                candidateConcessionsRepository.save(existing);
                return;
            }

            CandidateConcessionsEntity concessionsEntity = CandidateConcessionsEntity.builder()
                    .applicationId(savedApplication.getId())
                    .ageConcession(ageConcession)
                    .build();
            candidateConcessionsRepository.save(concessionsEntity);
        } catch (Exception ex) {
            logger.error("Error while upserting candidate concessions for applicationId={}: {}",
                    savedApplication.getId(), ex.getMessage(), ex);
        }
    }

    private boolean isAgeConcessionApplicable(UUID candidateId, UUID positionId, UUID stateId, UUID cityId) {
        JobPositionsEntity position = positionsRepository.findById(positionId).orElse(null);
        if (position == null) {
            logger.warn("Position not found while evaluating age concession for candidateId={}, positionId={}",
                    candidateId, positionId);
            return false;
        }

        // National jobs always evaluate against the single national age-relaxation row.
        if (!Boolean.TRUE.equals(position.getIsLocationWise())) {
            return ageRelaxationApplicationRepository.findByCandidateIdAndPositionId(candidateId, positionId)
                    .map(AgeRelaxationApplicationEntity::getRelaxationApplied)
                    .map(Boolean.TRUE::equals)
                    .orElse(false);
        }

        if (stateId == null) {
            logger.warn("State preference 1 is missing for location-wise job while evaluating age concession. candidateId={}, positionId={}",
                    candidateId, positionId);
            return false;
        }

        List<PositionStateDistributionEntity> stateDistributions = position.getPositionStateDistributions();
        boolean hasCitySpecificDistributionForState = stateDistributions != null && stateDistributions.stream()
                .anyMatch(dist -> stateId.equals(dist.getStateId()) && dist.getCityId() != null);

        Optional<AgeRelaxationApplicationEntity> ageRelaxationOpt;
        if (hasCitySpecificDistributionForState) {
            if (cityId == null) {
                logger.warn("City preference 1 is missing for city-specific state distribution. candidateId={}, positionId={}, stateId={}",
                        candidateId, positionId, stateId);
                return false;
            }
            ageRelaxationOpt = ageRelaxationApplicationRepository
                    .findByCandidateIdAndPositionIdAndStateIdAndCityId(candidateId, positionId, stateId, cityId);
        } else {
            ageRelaxationOpt = ageRelaxationApplicationRepository
                    .findByCandidateIdAndPositionIdAndStateIdAndCityId(candidateId, positionId, stateId, null);
        }

        return ageRelaxationOpt
                .map(AgeRelaxationApplicationEntity::getRelaxationApplied)
                .map(Boolean.TRUE::equals)
                .orElse(false);
    }

    private UUID resolveSelectedCityPreference(CandidateLocationPreferenceDTO candidateLocationPreferenceDTO) {
        if (candidateLocationPreferenceDTO.getCityPreference1() != null) {
            return candidateLocationPreferenceDTO.getCityPreference1();
        }
        return candidateLocationPreferenceDTO.getLocationPreference1();
    }

    @Transactional
    public CandidateOffersDTO updateOfferDecision(UpdateOfferDecisionModel model) {

        CandidateOffersEntity entity = candidateOffersRepository
                .findByCandidateApplication_Id(model.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No offer found for application"));

        if(!entity.getStatus().equals(CandidateOfferStatus.OFFER_SENT)){
            throw new CommonException("Invalid offer status. Offer not sent to this application");
        }

        entity.setMedicalCenterId(model.getMedicalCentreId());
        entity.setCandidateComments(model.getComments());
        entity.setStatus(model.getStatus());

        CandidateApplicationsEntity application = entity.getCandidateApplication();
        boolean isAccepted = model.getStatus().equals(CandidateOfferStatus.OFFER_ACCEPTED);
        application.setApplicationStatus(isAccepted
                ? CandidateApplicationStatus.OFFER_ACCEPTED
                : CandidateApplicationStatus.OFFER_REJECTED);
        candidateApplicationsRepository.saveWithWorkflow(application);

        // Increment offers_accepted statistics when candidate accepts
        if (isAccepted) {
            try {
                vacancyStatisticsService.incrementOffersAccepted(model.getApplicationId());
            } catch (Exception e) {
                log.warn("Failed to increment offers_accepted for applicationId {}: {}", model.getApplicationId(), e.getMessage());
            }
        }

        return candidateOffersMapper.toDTO(candidateOffersRepository.save(entity));
    }

}
