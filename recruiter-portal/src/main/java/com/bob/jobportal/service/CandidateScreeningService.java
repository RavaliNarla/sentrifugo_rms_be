package com.bob.jobportal.service;

import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.CommonMailService;
import com.bob.commonutil.service.PdfConverterService;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.commonutil.util.CustomMultipartFile;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.CandidateScreeningDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.*;
import com.bob.db.mapper.CandidateApplicationsMapper;
import com.bob.db.mapper.CandidateScreeningMapper;
import com.bob.db.mapper.InterviewCentresMapper;
import com.bob.db.model.CandidateApplicationWithRankProjection;
import com.bob.db.repository.*;
import com.bob.db.util.DBConstants;
import com.bob.jobportal.model.CandidateDetailsFilterRequestModel;
import com.bob.jobportal.model.CandidateDetailsResponseModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CandidateScreeningService {

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private CandidateApplicationsMapper candidateApplicationsMapper;

    @Autowired
    private CandidateProfileRepository profileRepository;

    @Autowired
    private WorkExperienceRepository workExperienceRepository;

    @Autowired
    private CandidateAddressRepository candidateAddressRepository;

    @Autowired
    private CandidateDocumentStoreRepository candidateDocumentStoreRepository;

    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    @Autowired
    private CandidateScreeningRepository candidateScreeningRepository;

    @Autowired
    private CandidateScreeningMapper candidateScreeningMapper;

    @Autowired
    private CandidateApplicationDocumentVerificationRepository candidateApplicationDocumentVerificationRepository;

    @Autowired
    private CommonMailService mailService;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

    @Autowired
    private JobRequisitionsRepository jobRequisitionsRepository;

    @Autowired
    private DepartmentsRepository departmentsRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;

    @Autowired
    private RequestTypesRepository requestTypesRepository;

    @Autowired
    private ConversationThreadsRepository conversationThreadsRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private PdfConverterService pdfConverterService;

    @Autowired
    private ConversationMessagesRepository conversationMessagesRepository;

    @Autowired
    CandidateRankingResultsRepository candidateRankingResultsRepository;

    @Autowired
    private CandidateLocationPreferencesRepository candidateLocationPreferencesRepository;

    @Autowired
    private InterviewCentresRepository interviewCentresRepository;

    @Autowired
    private InterviewCentresMapper interviewCentresMapper;

    @Autowired
    private CandidateWrittenExamMarksRepository candidateWrittenExamMarksRepositor;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewPanelMembersRepository panelMembersRepository;

    @Autowired
    private PositionPanelRepository positionPanelRepository;


    @Transactional(readOnly = true)
    public Page<CandidateDetailsResponseModel> getCandidateDetails(CandidateDetailsFilterRequestModel filter ) {

        Pageable pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize()
        );
        LocalDate currentDate = LocalDate.now();
        UUID userId=securityUtils.getCurrentUserId();
        String role=securityUtils.getCurrentUserRole();
        Optional<UserEntity> optUser=userRepository.findById(userId);
        List<UUID> positionIds=filter.getPositionIds();
        if(!optUser.isPresent()){
            return Page.empty();
        }
        if(role.equals(UserRole.COMMITTEE_MEMBER.toString())){
            List<InterviewPanelMembersEntity> panelMembersEntities=panelMembersRepository.findAllByPanelMember_Id(userId);
            if(panelMembersEntities.isEmpty()){
                return Page.empty();
            }
            List<UUID> panelIds=panelMembersEntities.stream().map(e->e.getPanel().getId()).toList();
            List<PositionPanelEntity> positionPanelEntities=positionPanelRepository.findActivePanels(panelIds,currentDate,List.of(PositionPanelStatus.APPROVED)).stream()
                    .filter(posPanel->AppConstants.SCREENING_COMMITTEE_NAME.equals(posPanel.getInterviewPanel().getCommittee().getCommitteeName())).toList();
            List<UUID> assignedPositionIds=positionPanelEntities.stream().map(PositionPanelEntity::getJobPosition).map(JobPositionsEntity::getId).toList();
            positionIds=positionIds.stream().filter(p->assignedPositionIds.contains(p)).toList();
             if(positionIds.isEmpty()){
                 return Page.empty();
            }
        }


        List<String> statusFilter =
                (filter.getStatus() == null || filter.getStatus().isEmpty())
                        ? null
                        : filter.getStatus()
                        .stream()
                        .map(Enum::name)
                        .toList();

        UUID stateId =
                (filter.getStateId() != null
                        && !filter.getStateId().toString().isEmpty())
                        ? filter.getStateId()
                        : null;

        UUID categoryId =
                (filter.getCategoryId() != null
                        && !filter.getCategoryId().toString().isEmpty())
                        ? filter.getCategoryId()
                        : null;

        /*
         * FETCH RANKED APPLICATIONS
         */
        Page<CandidateApplicationWithRankProjection> rankedApplications =
                candidateApplicationsRepository.findAppliedCandidatesByPositionWithRank(
                        filter.getSearchText(),
                        positionIds,
                        statusFilter,
                        stateId,
                        categoryId,
                        filter.getRank(),
                        pageable
                );

        /*
         * APPLICATION IDS
         */
        List<UUID> applicationIds = rankedApplications.stream()
                .map(CandidateApplicationWithRankProjection::getId)
                .toList();

        /*
         * FETCH FULL APPLICATION ENTITIES
         */
        List<CandidateApplicationsEntity> applicationEntities = candidateApplicationsRepository.findAllById(applicationIds);

        Map<UUID, CandidateApplicationsEntity> applicationMap = applicationEntities.stream()
                        .collect(Collectors.toMap(
                                CandidateApplicationsEntity::getId,
                                Function.identity()
                        ));

        /*
         * CANDIDATE IDS
         */
        List<UUID> candidateIds = rankedApplications.stream()
                .map(CandidateApplicationWithRankProjection::getCandidateId)
                .distinct()
                .toList();

        /*
         * RANKING RESULTS
         */
        List<CandidateRankingResultsEntity> candidateRankingResults = candidateRankingResultsRepository.findByApplicationIdIn(applicationIds);

        Map<UUID, CandidateRankingResultsEntity> mappedRankingResults =
                candidateRankingResults.stream()
                        .collect(Collectors.toMap(
                                CandidateRankingResultsEntity::getApplicationId,
                                Function.identity()
                        ));

        /*
         * PROFILE
         */
        List<CandidateProfileEntity> profileEntities = profileRepository.findAllByCandidateIdIn(candidateIds);

        Map<UUID, CandidateProfileEntity> mappedProfile =
                profileEntities.stream()
                        .collect(Collectors.toMap(
                                CandidateProfileEntity::getCandidateId,
                                Function.identity()
                        ));

        /*
         * WORK EXPERIENCE
         */
        List<WorkExperienceEntity> workexpEntities = workExperienceRepository.findAllByCandidateIdIn(candidateIds);

        Map<UUID, List<WorkExperienceEntity>> mappedWorkExp =
                workexpEntities.stream()
                        .collect(Collectors.groupingBy(
                                WorkExperienceEntity::getCandidateId
                        ));

        /*
         * LOCATION PREFERENCES
         */
        List<CandidateLocationPreferenceEntity> locationPreferenceEntities = candidateLocationPreferencesRepository.findByCandidateAndPosition(candidateIds, positionIds);

        Map<String, CandidateLocationPreferenceEntity> mappedLocationPreference =
                locationPreferenceEntities.stream()
                        .collect(Collectors.toMap(
                                c -> c.getCandidateId() + "_" + c.getPositionId(),
                                Function.identity()
                        ));

        /*
         * INTERVIEW CENTERS
         */
        List<UUID> zoneIds = locationPreferenceEntities.stream()
                .map(CandidateLocationPreferenceEntity::getInterviewCenter)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, InterviewCentresEntity> centerMap =
                interviewCentresRepository.findAllById(zoneIds)
                        .stream()
                        .collect(Collectors.toMap(
                                InterviewCentresEntity::getId,
                                Function.identity()
                        ));

        /*
         * RESUME DOCUMENTS
         */
        DocumentTypesEntity documentTypes = documentTypesRepository.findByDocCode(DocumentCode.RESUME.toString());

        List<CandidateDocumentStoreEntity> documentStoreEntities =
                candidateDocumentStoreRepository
                        .findAllByDocumentIdAndCandidateIdIn(
                                documentTypes.getId(),
                                candidateIds
                        );

        Map<UUID, CandidateDocumentStoreEntity> mappedDocumentStore =
                documentStoreEntities.stream()
                        .collect(Collectors.toMap(
                                CandidateDocumentStoreEntity::getCandidateId,
                                Function.identity()
                        ));

        /*
         * Writen Exam Marks
         */
        List<CandidateWrittenExamMarksEntity> writtenExamMarksEntities = candidateWrittenExamMarksRepositor.findByApplication_IdIn(applicationIds);
        Map<UUID, CandidateWrittenExamMarksEntity> mappedWrittenExamMarks = writtenExamMarksEntities.stream()
                        .collect(Collectors.toMap(
                                e -> e.getApplication().getId(),
                                Function.identity(),
                                (existing, replacement) -> existing
                        ));
        /*
         * FINAL RESPONSE
         */
        Page<CandidateDetailsResponseModel> response =
                rankedApplications.map(ranked -> {

                    CandidateApplicationsEntity entity =
                            applicationMap.get(ranked.getId());

                    CandidateProfileEntity profile =
                            mappedProfile.get(entity.getCandidateId());

                    CandidateLocationPreferenceEntity locationPreference =
                            mappedLocationPreference.get(
                                    entity.getCandidateId()
                                            + "_"
                                            + entity.getPositionId()
                            );

                    CandidateDocumentStoreEntity document =
                            mappedDocumentStore.get(entity.getCandidateId());

                    InterviewCentresEntity interviewCentre =
                            locationPreference != null
                                    ? centerMap.get(
                                    locationPreference.getInterviewCenter()
                            )
                                    : null;
                    CandidateWrittenExamMarksEntity writtenExamMarks = mappedWrittenExamMarks.get(entity.getId());


                    return CandidateDetailsResponseModel.builder()

                            .candidateApplications(
                                    candidateApplicationsMapper.toDTO(entity)
                            )

                            .categoryId(
                                    profile != null
                                            ? profile.getReservationCategoryId()
                                            : null
                            )

                            .stateId(
                                    locationPreference != null
                                            ? locationPreference.getStatePreference1()
                                            : null
                            )

                            .totalMonths(
                                    commonUtilityProvider.calculateTotalMonths(
                                            mappedWorkExp.get(entity.getCandidateId())
                                    )
                            )

                            .fullName(
                                    commonUtilityProvider.buildFullName(profile)
                            )

                            .resumeUrl(
                                    document != null
                                            ? document.getFileUrl()
                                            : null
                            )

                            .rank(
                                    Boolean.TRUE.equals(filter.getRank())
                                            ? ranked.getRankNumber()
                                            : null
                            )

                            .candidateRankingResults(
                                    Boolean.TRUE.equals(filter.getRank())
                                            ? mappedRankingResults.get(entity.getId())
                                            : null
                            )

                            .interviewCenter(
                                    interviewCentre != null
                                            ? interviewCentresMapper.toDto(interviewCentre)
                                            : null
                            )
                            .totalMarksObtained(
                                    writtenExamMarks != null ? writtenExamMarks.getTotalMarksObtained(): BigDecimal.ZERO
                            )
                            .examQualificationStatus(
                                    writtenExamMarks != null ? writtenExamMarks.getStatus() : null
                            )
                            .build();
                });

        return response;
    }

    public CandidateScreeningDTO getCandidateDiscrepancyDetails(UUID applicationId) {
        CandidateScreeningEntity entity = candidateScreeningRepository.findByApplicationId(applicationId).orElse(new CandidateScreeningEntity() );
        return candidateScreeningMapper.toDto(entity);
    }

    public CandidateScreeningDTO saveCandidateDiscrepancyDetails(CandidateScreeningDTO screeningDTO) {
        CandidateScreeningEntity entity =  candidateScreeningRepository.findByApplicationId(screeningDTO.getApplicationId()).orElse(new CandidateScreeningEntity() );
        candidateScreeningMapper.updateEntityFromDto(screeningDTO,entity);
        CandidateApplicationsEntity applicationsEntity = candidateApplicationsRepository.findById(screeningDTO.getApplicationId()).orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        boolean hasAdditionalRequiredDocs = createAdditionalRequiredDocuments(screeningDTO);
        if (Boolean.TRUE.equals(entity.getIsEligible())) {
            applicationsEntity.setApplicationStatus(CandidateApplicationStatus.ELIGIBLE);
        } else if(screeningDTO.getIsShortlisted() == CandidateScreeningShortlistedStatus.YES){
            applicationsEntity.setApplicationStatus(CandidateApplicationStatus.SHORTLISTED);
        } else if(screeningDTO.getIsShortlisted() == CandidateScreeningShortlistedStatus.NO){
            applicationsEntity.setApplicationStatus(CandidateApplicationStatus.REJECTED);
        } else if (screeningDTO.getIsAgeCriteriaMet() == CandidateScreeningCriteriaStatus.DISCREPANCY
                || screeningDTO.getIsWorkCriteriaMet() == CandidateScreeningCriteriaStatus.DISCREPANCY
                || screeningDTO.getIsEducationCriteriaMet() == CandidateScreeningCriteriaStatus.DISCREPANCY
                || hasAdditionalRequiredDocs){
            applicationsEntity.setApplicationStatus(CandidateApplicationStatus.DISCREPANCY);
        }

        sendEmailIfDiscrepancyFound(screeningDTO, applicationsEntity, hasAdditionalRequiredDocs);
        CandidateScreeningEntity savedentity  = candidateScreeningRepository.save(entity);
        candidateApplicationsRepository.saveWithWorkflow(applicationsEntity);
        return candidateScreeningMapper.toDto(savedentity);
    }

    private boolean createAdditionalRequiredDocuments(CandidateScreeningDTO screeningDTO) {
        List<String> additionalDocumentNames = screeningDTO.getAdditionalDocumentNames();
        if (additionalDocumentNames == null || additionalDocumentNames.isEmpty()) {
            return false;
        }

        DocumentTypesEntity additionalDocType = documentTypesRepository.findByDocCode("ADDRD");
        if (additionalDocType == null) {
            throw new ResourceNotFoundException("Document type with code ADDRD not found");
        }

        UUID currentUserId = securityUtils.getCurrentUserId();
        List<CandidateApplicationDocumentVerificationEntity> additionalDocs = additionalDocumentNames.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(name -> CandidateApplicationDocumentVerificationEntity.builder()
                        .candidateDocumentId(null)
                        .candidateId(screeningDTO.getCandidateId())
                        .applicationId(screeningDTO.getApplicationId())
                        .docScreeningStatus(DocumentScreeningStatus.REJECTED)
                        .docScreeningComments(null)
                        .lastScreenedByUserId(currentUserId)
                        .zonalHrDocStatus(DocumentZonalVerificationStatus.PENDING)
                        .zonalHrDocComments(null)
                        .documentId(additionalDocType.getId())
                        .fileUrl(null)
                        .displayName(name)
                        .isValidationPending(Boolean.FALSE)
                        .documentNumber(null)
                        .pendingChecks(null)
                        .build())
                .toList();

        if (additionalDocs.isEmpty()) {
            return false;
        }

        candidateApplicationDocumentVerificationRepository.saveAll(additionalDocs);
        return true;
    }


//    public void SendMessage(CandidateScreeningDTO screeningDTO, CandidateApplicationsEntity applicationsEntity){
//
//        RequestTypesEntity entity = requestTypesRepository.findByRequestName(DBConstants.DESCRANCY).orElseThrow(() -> new ResourceNotFoundException("Request type not found"));
//        ConversationThreadsEntity threads = ConversationThreadsEntity.builder()
//                .applicationId(applicationsEntity.getId())
//                .status(ConversationThreadsStatus.PENDING)
//                .requestTypeId(entity.getId())
//                .initiatedBy(DBConstants.HEDAER_RECRUITER)
//                .build();
//
//        ConversationThreadsEntity savedThread =conversationThreadsRepository.save(threads);
//
//        ConversationMessagesEntity conversationMessagesEntity= ConversationMessagesEntity.builder()
//                .threadId(savedThread.getId())
//                .message("Discrepancy found in application. update your documents on or before "+screeningDTO.getSubmitBeforeDate())
//                .senderId(securityUtils.getCurrentUserId())
//                .senderType(DBConstants.HEDAER_RECRUITER)
//                .build();
//        conversationMessagesRepository.save(conversationMessagesEntity);
//
//    }

    public void sendEmailIfDiscrepancyFound(
            CandidateScreeningDTO screeningDTO,
            CandidateApplicationsEntity applicationsEntity,
            boolean hasAdditionalRequiredDocs
    ){

        Map<String, String> descrepancyList = new HashMap<>();
        if(screeningDTO.getIsWorkCriteriaMet() == CandidateScreeningCriteriaStatus.DISCREPANCY){
            descrepancyList.put(DBConstants.DESCRANCY_WORK_EXPERIENCE,screeningDTO.getWorkCriteriaRemark());
        }
        if(screeningDTO.getIsAgeCriteriaMet() == CandidateScreeningCriteriaStatus.DISCREPANCY){
            descrepancyList.put(AppConstants.AGE_DISCREPANCY,screeningDTO.getAgeCriteriaRemark());
        }
        if(screeningDTO.getIsEducationCriteriaMet() == CandidateScreeningCriteriaStatus.DISCREPANCY){
            descrepancyList.put(AppConstants.EDUCATION_DISCREPANCY,screeningDTO.getEducationCriteriaRemark());
        }
        if(descrepancyList.size() != 0 || hasAdditionalRequiredDocs){

            if(screeningDTO.getSubmitBeforeDate() == null){
                throw new IllegalArgumentException("Submit before date cannot be null");
            }
//            SendMessage(screeningDTO,applicationsEntity);
            CandidateProfileEntity candidatesEntity = profileRepository.findByCandidateId(screeningDTO.getCandidateId()).orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));
            CandidateAddressEntity addressEntity = candidateAddressRepository.findByCandidateId(screeningDTO.getCandidateId())
                    .orElseThrow(()->new ResourceNotFoundException("Candidate Address not found"));

            StateEntity stateEntity = stateRepository.findById(addressEntity.getStateId()).orElseThrow(()->new ResourceNotFoundException("State not found"));
            DistrictEntity districtEntity = districtRepository.findById(addressEntity.getDistrictId()).orElseThrow(()->new ResourceNotFoundException("District not Found"));

            JobPositionsEntity positionsEntity = positionsRepository.findById(applicationsEntity.getPositionId()).orElseThrow(() -> new ResourceNotFoundException("Position not found"));
            MasterPositionsEntity masterPositionsEntity = masterPositionsRepository.findById(positionsEntity.getMasterPositionId()).orElseThrow(() -> new ResourceNotFoundException("Master Position not found"));
            JobRequisitionsEntity jobRequisitionsEntity = jobRequisitionsRepository.findById(positionsEntity.getRequisitionId()).orElseThrow(()->new ResourceNotFoundException("Requisition not found"));
            DepartmentsEntity departmentsEntity = departmentsRepository.findById(positionsEntity.getDeptId()).orElseThrow(()->new ResourceNotFoundException("Department not found"));

            List<CandidateApplicationDocumentVerificationEntity> rejectedDocs =
                    candidateApplicationDocumentVerificationRepository.findByApplicationId(applicationsEntity.getId())
                            .stream()
                            .filter(e -> DocumentScreeningStatus.REJECTED == e.getDocScreeningStatus())
                            .toList();

            Set<UUID> candidateDocumentIds = rejectedDocs.stream()
                    .map(CandidateApplicationDocumentVerificationEntity::getCandidateDocumentId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<UUID,CandidateDocumentStoreEntity> candidateDocumentStoreMap = candidateDocumentStoreRepository.findAllById(candidateDocumentIds)
                    .stream()
                    .collect(Collectors.toMap(
                            CandidateDocumentStoreEntity::getId,
                            Function.identity()
                    ));


            Set<UUID> documentTypeIds = candidateDocumentStoreMap.values().stream().
                    map(CandidateDocumentStoreEntity::getDocumentId)
                    .collect(Collectors.toSet());
            Map<UUID,String> documentNamesMap = documentTypesRepository.findAllById(documentTypeIds).stream()
                    .collect(
                            Collectors.toMap(
                                    DocumentTypesEntity::getId,
                                    DocumentTypesEntity::getDocumentName
                            )
                    );

            // Build document list with names and comments
            List<Map<String, String>> documentList = rejectedDocs.stream()
                    .map(doc -> {
                        // Get document name from candidate_document_store -> document_types
                        CandidateDocumentStoreEntity candidateDocumentStore = candidateDocumentStoreMap.get(doc.getCandidateDocumentId());
                        String documentName;
                        if (candidateDocumentStore != null) {
                            documentName = documentNamesMap.getOrDefault(candidateDocumentStore.getDocumentId(), AppConstants.UNKNOWN_DOCUMENT);
                        } else if (doc.getDisplayName() != null && !doc.getDisplayName().isBlank()) {
                            // Additional required docs don't have candidate_document_id; use explicit display name.
                            documentName = doc.getDisplayName();
                        } else {
                            documentName = AppConstants.UNKNOWN_DOCUMENT;
                        }

                        Map<String, String> docMap = new HashMap<>();
                        docMap.put(AppConstants.DOCUMENT_NAME, documentName);
                        docMap.put(AppConstants.COMMENTS, doc.getDocScreeningComments() != null ? doc.getDocScreeningComments() : "");
                        return docMap;
                    })
                    .toList();

            Context context = new Context();
            context.setVariable(DBConstants.CANDIDATE_NAME, commonUtilityProvider.buildFullName(candidatesEntity));
            context.setVariable(DBConstants.APPLICATION_NO, applicationsEntity.getApplicationNo());
            context.setVariable(DBConstants.POSITION_NAME, masterPositionsEntity.getPositionName());
            context.setVariable(AppConstants.REJECTED_DOCUMENTS,documentList);
            context.setVariable(DBConstants.DESCRANCY_LIST, descrepancyList);
            context.setVariable(DBConstants.DEADLINE_DATE, screeningDTO.getSubmitBeforeDate());
            context.setVariable(DBConstants.COMPANY_NAME, AppConstants.BOB_RECRUITMENT);
            context.setVariable(AppConstants.APPLICATION_DATE,applicationsEntity.getApplicationDate());
            context.setVariable(AppConstants.CANDIDATE_LAST_NAME,candidatesEntity.getLastName());
            context.setVariable(AppConstants.CANDIDATE_ADDRESS,addressEntity);
            context.setVariable(AppConstants.STATE_NAME,stateEntity.getStateName());
            context.setVariable(AppConstants.DISTRICT_NAME,districtEntity.getDistrictName());
            context.setVariable(AppConstants.ADVERTISEMENT_START_DATE,jobRequisitionsEntity.getStartDate());
            context.setVariable(AppConstants.DEPARTMENT,departmentsEntity.getDepartmentName());


            String htmlContent = templateEngine.process(DBConstants.DESCRANCY_EMAIL_TEMPLATE, context);
            String pdfContent = templateEngine.process(AppConstants.DISCREPANCY_EMAIL_ATTACHMENT,context);
            byte[] pdfBytes = pdfConverterService.convertHtmlStringToPdf(pdfContent);
            MultipartFile pdfFile = new CustomMultipartFile(pdfBytes, "file", "discrepancy-attachment.pdf", "application/pdf");
            mailService.sendEmailTempleteFile(candidatesEntity.getEmail(), DBConstants.DESCRANCY_MESSAGE_SUBJECT,htmlContent,pdfFile);


        }
    }
}
