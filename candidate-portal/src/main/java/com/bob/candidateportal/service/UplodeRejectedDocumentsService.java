package com.bob.candidateportal.service;


import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.candidateportal.model.ScreeningAndZonalRejectedDocuments;
import com.bob.commonutil.service.CommonMailService;
import com.bob.commonutil.service.FileService;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.CandidateApplicationDocumentVerificationDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.*;
import com.bob.db.mapper.CandidateApplicationDocumentVerificationMapper;
import com.bob.db.repository.*;
import com.bob.db.util.DBConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UplodeRejectedDocumentsService {

    @Autowired
    private CandidateApplicationDocumentVerificationRepository candidateApplicationDocumentVerificationRepository;

    @Autowired
    private CandidateDocumentStoreRepository candidateDocumentStoreRepository;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private CandidateScreeningRepository candidateScreeningRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private CommonMailService commonMailService;

    @Autowired
    private FileService fileService;
//
//    @Autowired
//    private ConversationThreadsRepository conversationThreadsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RequestTypesRepository requestTypesRepository;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private CandidateApplicationDocumentVerificationMapper candidateApplicationDocumentVerificationMapper;

    @Autowired
    private InterviewScheduleRepository interviewScheduleRepository;


    @Value("${candidate.document.upload.path}")
    private String CANDIDATE_DOCUMENT_FOLDER;

    @Autowired
    private CandidateLocationPreferencesRepository candidateLocationPreferencesRepository;

    @Autowired
    private WorkflowApprovalEntityRepository workflowApprovalEntityRepository;

    @Autowired
    private SecurityUtils securityUtils;

    public ScreeningAndZonalRejectedDocuments getRejectedDocuments(UUID applicationId) {
        CandidateScreeningEntity candidateScreeningEntity = candidateScreeningRepository.findByApplicationId(applicationId)
                .orElseThrow(()->new ResourceNotFoundException("Candidate Screening not found"));
        List<CandidateApplicationDocumentVerificationEntity> result = candidateApplicationDocumentVerificationRepository.findByApplicationIdAndDocScreeningStatus(applicationId, DocumentScreeningStatus.REJECTED);
        return ScreeningAndZonalRejectedDocuments.builder()
                .RejectedDocuments(
                        candidateApplicationDocumentVerificationMapper.toDtoList(result).stream()
                                .map(this::enrichAdditionalDocIdentifiers)
                                .toList()
                )
                .submitBeforeDate(candidateScreeningEntity.getSubmitBeforeDate())
                .build();
    }


    public boolean checkIfSubmitBeforeDatePassed(LocalDate submitBeforeDate) {
        return submitBeforeDate != null && LocalDate.now().isAfter(submitBeforeDate);
    }

    @Transactional
    public CandidateApplicationDocumentVerificationDTO uploadRejectedDocuments(UUID verificationId, MultipartFile file) throws IOException {
        CandidateApplicationDocumentVerificationEntity entity = candidateApplicationDocumentVerificationRepository.findById(verificationId)
                .orElseThrow(()->new ResourceNotFoundException("Document not found"));

        CandidateScreeningEntity candidateScreeningEntity = candidateScreeningRepository.findByApplicationId(entity.getApplicationId())
                .orElseThrow(()->new ResourceNotFoundException("Candidate Screening not found"));

        LocalDate submitBeforeDate = candidateScreeningEntity.getSubmitBeforeDate();

        if (checkIfSubmitBeforeDatePassed(submitBeforeDate)) {
            log.error("Submit before date has passed. Deadline: {}", submitBeforeDate);
            throw new IllegalStateException("Submit before date has passed");
        }

        CandidateDocumentStoreEntity storeEntity = null;
        if (entity.getCandidateDocumentId() != null) {
            storeEntity = candidateDocumentStoreRepository.findById(entity.getCandidateDocumentId())
                    .orElse(null);
        }

        CandidateApplicationsEntity applicationsEntitie = candidateApplicationsRepository.findById(entity.getApplicationId())
                .orElseThrow(()->new ResourceNotFoundException("Application not found"));

        String path = fileService.uploadFile(file, DBConstants.CANDIDATE+UUID.randomUUID(),CANDIDATE_DOCUMENT_FOLDER);
        String fullPath =CANDIDATE_DOCUMENT_FOLDER+"/"+path;

        // updating document
        entity.setDocScreeningStatus(DocumentScreeningStatus.PENDING);
        entity.setFileUrl(fullPath);

        // update in candidate document store


        CandidateApplicationDocumentVerificationEntity savedEntity = candidateApplicationDocumentVerificationRepository.save(entity);

        if(storeEntity != null) {
            storeEntity.setFileUrl(fullPath);
            candidateDocumentStoreRepository.save(storeEntity);
        }
            try {
                updateAllApplicationStatus(List.of(entity.getApplicationId()), Map.of(applicationsEntitie.getId(), submitBeforeDate));
                if (entity.getCandidateDocumentId() != null) {
                    updateAllDiscrepancyFileWithSameCandidateDocumentIdScreening(entity.getCandidateDocumentId(), fullPath);
                }
            } catch (Exception e) {
                log.error("Error during post-upload processing: {}", e.getMessage());
            }


        return enrichAdditionalDocIdentifiers(candidateApplicationDocumentVerificationMapper.toDto(savedEntity));
    }

    public List<CandidateApplicationsEntity> updateAllApplicationStatus(List<UUID> applicationIds,Map<UUID, LocalDate> screeningMap) {
        List<CandidateApplicationDocumentVerificationEntity> candidateApplicationDocumentVerificationEntities = candidateApplicationDocumentVerificationRepository
                .findAllByApplicationIdInAndDocScreeningStatus(applicationIds,DocumentScreeningStatus.REJECTED);

        // Step 1: Count rejected documents
        Map<UUID, Long> rejectCountMap =
                candidateApplicationDocumentVerificationEntities.stream()
                        .collect(Collectors.groupingBy(
                                CandidateApplicationDocumentVerificationEntity::getApplicationId,
                                Collectors.counting()
                        ));


        applicationIds.forEach(appId ->  rejectCountMap.putIfAbsent(appId, 0L));

        List<UUID> listOfApplication =
                rejectCountMap.entrySet().stream()
                        .filter(entry -> entry.getValue() == 0)
                        .map(Map.Entry::getKey)
                        .toList();

        if(listOfApplication.isEmpty()){
            log.info("No applications with all documents approved. No status updates will be made.");
            return Collections.emptyList();
        }


        List<CandidateApplicationsEntity> applicationsEntities = candidateApplicationsRepository.findAllById(listOfApplication);

        for (CandidateApplicationsEntity app : applicationsEntities) {
            app.setApplicationStatus(CandidateApplicationStatus.PENDING);
        }

        List<CandidateApplicationsEntity> entities = candidateApplicationsRepository.saveAllWithWorkflow(applicationsEntities);

        try {
            sendMailAndMessageScreening(listOfApplication,screeningMap);
        } catch (Exception e) {
            log.error("Error sending emails after application status update: {}", e.getMessage());
        }

        return entities;
    }

    public void updateAllDiscrepancyFileWithSameCandidateDocumentIdScreening(UUID candidateDocumentId,String fullPath) {

        List<CandidateApplicationDocumentVerificationEntity> entities =
                candidateApplicationDocumentVerificationRepository
                        .findByCandidateDocumentIdAndDocScreeningStatus(
                                candidateDocumentId,
                                DocumentScreeningStatus.REJECTED
                        );

        List<UUID> applicationIds = entities.stream()
                .map(CandidateApplicationDocumentVerificationEntity::getApplicationId)
                .toList();

        List<CandidateScreeningEntity> candidateScreeningEntities =
                candidateScreeningRepository.findAllByApplicationIdIn(applicationIds);


        List<UUID> applicationIdsWithValidScreening =
                candidateScreeningEntities.stream()
                        .filter(e -> !checkIfSubmitBeforeDatePassed(e.getSubmitBeforeDate()))
                        .map(CandidateScreeningEntity::getApplicationId)
                        .collect(Collectors.toList());

        if (applicationIdsWithValidScreening.isEmpty()) {
            log.info("No applications with valid screening found. No updates will be made.");
            return;
        }

        Map<UUID, LocalDate> screeningMap =
                candidateScreeningEntities.stream()
                        .collect(Collectors.toMap(
                                CandidateScreeningEntity::getApplicationId,
                                CandidateScreeningEntity::getSubmitBeforeDate,
                                (existing, replacement) -> existing
                        ));

        for (CandidateApplicationDocumentVerificationEntity entity : entities) {
            if (applicationIdsWithValidScreening.contains(entity.getApplicationId())) {
                entity.setDocScreeningStatus(DocumentScreeningStatus.PENDING);
                entity.setFileUrl(fullPath);
            }
        }

        candidateApplicationDocumentVerificationRepository.saveAll(entities);
        updateAllApplicationStatus(applicationIdsWithValidScreening, screeningMap);
    }

    public void sendMailAndMessageScreening(List<UUID> listOfApplication , Map<UUID,LocalDate> screeningMap){

        try{
            List<CandidateApplicationsEntity> applicationsEntities= candidateApplicationsRepository.findAllById(listOfApplication);

            List<WorkflowApprovalEntity> workflowApprovalEntities = workflowApprovalEntityRepository.findLatestByEntityIds(CandidateApplicationsEntity.ENTITY_TYPE,listOfApplication,CandidateApplicationStatus.DISCREPANCY.toString());

            Map<UUID, UUID> applicationUserMap =
                    workflowApprovalEntities.stream()
                            .filter(t -> t.getCreatedBy() != null &&
                                    !t.getCreatedBy().equals(
                                            UUID.fromString("00000000-0000-0000-0000-000000000000")))
                            .collect(Collectors.toMap(
                                    WorkflowApprovalEntity::getEntityId,
                                    WorkflowApprovalEntity::getCreatedBy, //TODO need to change this to approver id
                                    (existing, replacement) -> existing
                            ));

            List<UUID> listOfUsers = applicationUserMap.values().stream().filter(Objects::nonNull).distinct().toList();

            List<UserEntity> userEntity = userRepository.findAllById(listOfUsers);
            Map<UUID,String> userNameMap = userEntity.stream()
                    .collect(Collectors.toMap(
                            UserEntity::getId,
                            UserEntity::getEmail,
                            (existing, replacement) -> existing
                    ));


            List<CandidateProfileEntity> candidateProfileEntities = candidateProfileRepository.findAllByCandidateIdIn(
                    applicationsEntities.stream()
                            .map(CandidateApplicationsEntity::getCandidateId)
                            .toList()
            );
            Map<UUID, CandidateProfileEntity> candidateNameMap =
                    candidateProfileEntities.stream()
                            .collect(Collectors.toMap(
                                    CandidateProfileEntity::getCandidateId,
                                    Function.identity(),
                                    (existing, replacement) -> existing
                            ));


            List<UUID> listOfPostions = applicationsEntities.stream().map(CandidateApplicationsEntity::getPositionId).toList();
            List<JobPositionsEntity> listOfJoPositions = positionsRepository.findAllById(listOfPostions);

            List<UUID> listOfMasterPositionsIds = listOfJoPositions.stream().map(JobPositionsEntity::getMasterPositionId).toList();
            List<MasterPositionsEntity> listOfMasterPositions = masterPositionsRepository.findAllByIdIn(listOfMasterPositionsIds);

            Map<UUID, String> masterPositionNameMap =
                    listOfMasterPositions.stream()
                            .collect(Collectors.toMap(
                                    MasterPositionsEntity::getId,
                                    MasterPositionsEntity::getPositionName,
                                    (existing, replacement) -> existing
                            ));

            Map<UUID, String> positionNameMap =
                    listOfJoPositions.stream()
                            .collect(Collectors.toMap(
                                    JobPositionsEntity::getId,
                                    jp -> masterPositionNameMap.get(jp.getMasterPositionId()),
                                    (existing, replacement) -> existing
                            ));

            for (CandidateApplicationsEntity applicationEntity : applicationsEntities){

                Context context = new Context();
                context.setVariable(AppConstants.APPLICATION_NO, applicationEntity.getApplicationNo());
                context.setVariable(AppConstants.POSITION_NAME, positionNameMap.get(applicationEntity.getPositionId()));
                context.setVariable(AppConstants.DEADLINE_DATE, screeningMap.get(applicationEntity.getId()).format(AppConstants.DD_MM_YYYY));
                context.setVariable(AppConstants.CANDIDATE_NAME, commonUtilityProvider.buildFullName(candidateNameMap.get(applicationEntity.getCandidateId())));
                context.setVariable(AppConstants.CANDIDATE_EMAIL, candidateNameMap.get(applicationEntity.getCandidateId()).getEmail());
                context.setVariable(AppConstants.ADDITIONAL_NODE,"Please confirm if any extra documents are required.");
                String htmlBody = templateEngine.process(AppConstants.DISCREPANCY_EMAIL_TEMPLATE, context);
                UUID userId = applicationUserMap.get(applicationEntity.getId());
                commonMailService.sendEmailTempleteFile(userNameMap.get(userId),"Candidate updated documents",htmlBody,null);
            }
        } catch (Exception e) {
            log.error("Error sending emails: {}", e.getMessage());
        }
    }

    // Zonal Rejected Documents
    public ScreeningAndZonalRejectedDocuments getRejectedDocumentsZonal(UUID applicationId) {
        InterviewScheduleEntity  interviewScheduleEntity = interviewScheduleRepository.findByApplicationId(applicationId)
                .orElseThrow(()->new ResourceNotFoundException("Interview Schedule not found"));

        List<CandidateApplicationDocumentVerificationEntity> result =  candidateApplicationDocumentVerificationRepository.findByApplicationIdAndZonalHrDocStatus(applicationId, DocumentZonalVerificationStatus.REJECTED);
        return ScreeningAndZonalRejectedDocuments.builder()
                .RejectedDocuments(
                        candidateApplicationDocumentVerificationMapper.toDtoList(result).stream()
                                .map(this::enrichAdditionalDocIdentifiers)
                                .toList()
                )
                .submitBeforeDate(interviewScheduleEntity.getZonalSubmitBeforeDate())
                .build();
    }

    public CandidateApplicationDocumentVerificationDTO uplodeRejectedDocumentsZonal(UUID verificationId, MultipartFile file) throws IOException {
        CandidateApplicationDocumentVerificationEntity canAppDocEntity = candidateApplicationDocumentVerificationRepository.findById(verificationId)
                .orElseThrow(()->new ResourceNotFoundException("Document not found"));

        InterviewScheduleEntity  interviewScheduleEntity = interviewScheduleRepository.findByApplicationId(canAppDocEntity.getApplicationId())
                .orElseThrow(()->new ResourceNotFoundException("Interview Schedule not found"));

        LocalDate submitBeforeDate = interviewScheduleEntity.getZonalSubmitBeforeDate();

        if (checkIfSubmitBeforeDatePassed(submitBeforeDate)) {
            log.info("Submit before date has passed. Deadline: {}", submitBeforeDate);
            throw new CommonException("Submit before date has passed");
        }

        CandidateDocumentStoreEntity storeEntity = null;
        if (canAppDocEntity.getCandidateDocumentId() != null) {
            storeEntity = candidateDocumentStoreRepository.findById(canAppDocEntity.getCandidateDocumentId())
                    .orElse(null);
        }

        CandidateApplicationsEntity applicationsEntitie = candidateApplicationsRepository.findById(canAppDocEntity.getApplicationId())
                .orElseThrow(()->new ResourceNotFoundException("Application not found"));

        String path = fileService.uploadFile(file, DBConstants.CANDIDATE+UUID.randomUUID(),CANDIDATE_DOCUMENT_FOLDER);
        String fullPath =CANDIDATE_DOCUMENT_FOLDER+"/"+path;

        // updating document
        canAppDocEntity.setZonalHrDocStatus(DocumentZonalVerificationStatus.PENDING);
        canAppDocEntity.setFileUrl(fullPath);

        if(storeEntity != null) {
            // update in candidate document store
            storeEntity.setFileUrl(fullPath);
            candidateDocumentStoreRepository.save(storeEntity);
        }
        CandidateApplicationDocumentVerificationEntity savedEntity = candidateApplicationDocumentVerificationRepository.save(canAppDocEntity);
        try {
            updateAllInterviewStatus(List.of(canAppDocEntity.getApplicationId()), Map.of(applicationsEntitie.getId(), submitBeforeDate));
            if (canAppDocEntity.getCandidateDocumentId() != null) {
                updateAllDiscrepancyFileWithSameCandidateDocumentIdZonal(canAppDocEntity.getCandidateDocumentId(), fullPath);
            }
        }catch(Exception e){
            log.info("Update failed:{}",e.getMessage());
        }
        return enrichAdditionalDocIdentifiers(candidateApplicationDocumentVerificationMapper.toDto(savedEntity));
    }

    public void updateAllDiscrepancyFileWithSameCandidateDocumentIdZonal(UUID candidateDocumentId,String fullPath){
        List<CandidateApplicationDocumentVerificationEntity> entities =
                candidateApplicationDocumentVerificationRepository
                        .findByCandidateDocumentIdAndZonalHrDocStatus(
                                candidateDocumentId,
                                DocumentZonalVerificationStatus.REJECTED
                        );

        List<UUID> applicationIds = entities.stream()
                .map(CandidateApplicationDocumentVerificationEntity::getApplicationId)
                .toList();

        List<InterviewScheduleEntity>  interviewScheduleEntities = interviewScheduleRepository.findByApplicationIdIn(applicationIds);


        Map<UUID, LocalDate> submitDateMap =
                interviewScheduleEntities.stream()
                        .filter(e -> {
                            LocalDate submitDate = e.getZonalSubmitBeforeDate();
                            return submitDate != null &&
                                    !LocalDate.now().isAfter(submitDate);
                        })
                        .collect(Collectors.toMap(
                                InterviewScheduleEntity::getApplicationId,
                                InterviewScheduleEntity::getZonalSubmitBeforeDate,
                                (existing, replacement) -> existing
                        ));
        List<UUID> applicationIdsWithValidZonal =  submitDateMap.keySet().stream().toList();

        for (CandidateApplicationDocumentVerificationEntity entity : entities) {
            if (applicationIdsWithValidZonal.contains(entity.getApplicationId())) {
                entity.setZonalHrDocStatus(DocumentZonalVerificationStatus.PENDING);
                entity.setFileUrl(fullPath);
            }
        }

        candidateApplicationDocumentVerificationRepository.saveAll(entities);
    }
    public void updateAllInterviewStatus(List<UUID> applicationId, Map<UUID, LocalDate> screeningMap) {
        List<CandidateApplicationDocumentVerificationEntity> candidateApplicationDocumentVerificationEntities = candidateApplicationDocumentVerificationRepository
                .findAllByApplicationIdInAndZonalHrDocStatus(applicationId,DocumentZonalVerificationStatus.REJECTED);

        Map<UUID, Long> rejectCountMap =
                candidateApplicationDocumentVerificationEntities.stream()
                        .collect(Collectors.groupingBy(
                                CandidateApplicationDocumentVerificationEntity::getApplicationId,
                                Collectors.counting()
                        ));


        applicationId.forEach(appId -> rejectCountMap.putIfAbsent(appId, 0L));

        List<UUID> listOfApplication =
                rejectCountMap.entrySet().stream()
                        .filter(entry -> entry.getValue() == 0 )
                        .map(Map.Entry::getKey)
                        .toList();

        List<InterviewScheduleEntity> interviewScheduleEntities = interviewScheduleRepository.findByApplicationIdIn(listOfApplication);

        // Create old schedule copies for audit trail
        List<InterviewScheduleEntity> oldSchedules = new ArrayList<>();
        for (InterviewScheduleEntity schedule : interviewScheduleEntities) {
            InterviewScheduleEntity oldSchedule = InterviewScheduleEntity.builder()
                    .applicationId(schedule.getApplicationId())
                    .candidateId(schedule.getCandidateId())
                    .panelId(schedule.getPanelId())
                    .interviewStartAt(schedule.getInterviewStartAt())
                    .interviewEndAt(schedule.getInterviewEndAt())
                    .interviewDurationMinutes(schedule.getInterviewDurationMinutes())
                    .meetingLink(schedule.getMeetingLink())
                    .zonalOfficeId(schedule.getZonalOfficeId())
                    .finalScore(schedule.getFinalScore())
                    .interviewStatus(schedule.getInterviewStatus())
                    .zonalVerificationStatus(schedule.getZonalVerificationStatus())
                    .zonalSubmitBeforeDate(schedule.getZonalSubmitBeforeDate())
                    .zonalHrComments(schedule.getZonalHrComments())
                    .lptRequired(schedule.getLptRequired())
                    .lptStatus(schedule.getLptStatus())
                    .mailSendStatus(schedule.getMailSendStatus())
                    .mailSentOn(schedule.getMailSentOn())
                    .mailComments(schedule.getMailComments())
                    .build();
            oldSchedule.setId(schedule.getId());
            oldSchedules.add(oldSchedule);
        }

        for(InterviewScheduleEntity entity : interviewScheduleEntities){
            entity.setZonalVerificationStatus(ZonalVerificationStatus.PENDING);
            entity.setInterviewStatus(InterviewSchedulingStatus.PENDING);
        }

        interviewScheduleRepository.saveAllWithAudit(oldSchedules, interviewScheduleEntities);
        try {
            sendMailAndMessageZonal(listOfApplication, screeningMap);
        }catch(Exception e){
            log.info("Couldn't send mail to Zonal HR:{}",e.getMessage());
        }
    }

    public void sendMailAndMessageZonal(List<UUID> listOfApplication, Map<UUID,LocalDate> screeningMap){

        List<CandidateApplicationsEntity> applicationsEntities= candidateApplicationsRepository.findAllById(listOfApplication);
        List<UUID> positionIds = applicationsEntities.stream().map(CandidateApplicationsEntity::getPositionId).toList();
        UUID candidateId= applicationsEntities.get(0).getCandidateId();
        List<CandidateLocationPreferenceEntity> locationPreferenceEntities = candidateLocationPreferencesRepository
                .findByCandidateIdAndPositionIdIn(candidateId, positionIds);

        Map<UUID, UUID> positionLocationMap = locationPreferenceEntities.stream()
                .collect(Collectors.toMap(
                        CandidateLocationPreferenceEntity::getPositionId,
                        CandidateLocationPreferenceEntity::getInterviewCenter,
                        (existing, replacement) -> existing
                ));
        List<UUID> listOfUsers = locationPreferenceEntities.stream().map( CandidateLocationPreferenceEntity::getInterviewCenter).toList();

        List<UserEntity> userEntity = userRepository.findByRoleAndInterviewCenterIdIn(AppConstants.ZONAL_HR_ROLE,listOfUsers);


        Map<UUID,String> userNameMap = userEntity.stream()
                .collect(Collectors.toMap(
                        UserEntity::getInterviewCenterId,
                        UserEntity::getEmail,
                        (existing, replacement) -> existing
                ));



        List<CandidateProfileEntity> candidateProfileEntities = candidateProfileRepository.findAllByCandidateIdIn(
                applicationsEntities.stream()
                        .map(CandidateApplicationsEntity::getCandidateId)
                        .toList()
        );

        Map<UUID, CandidateProfileEntity> candidateNameMap =
                candidateProfileEntities.stream()
                        .collect(Collectors.toMap(
                                CandidateProfileEntity::getCandidateId,
                                Function.identity(),
                                (existing, replacement) -> existing
                        ));


        List<UUID> listOfPostions = applicationsEntities.stream().map(CandidateApplicationsEntity::getPositionId).toList();
        List<JobPositionsEntity> listOfJoPositions = positionsRepository.findAllById(listOfPostions);

        List<UUID> listOfMasterPositionsIds = listOfJoPositions.stream().map(JobPositionsEntity::getMasterPositionId).toList();
        List<MasterPositionsEntity> listOfMasterPositions = masterPositionsRepository.findAllByIdIn(listOfMasterPositionsIds);
        Map<UUID, String> masterPositionNameMap =
                listOfMasterPositions.stream()
                        .collect(Collectors.toMap(
                                MasterPositionsEntity::getId,
                                MasterPositionsEntity::getPositionName,
                                (existing, replacement) -> existing
                        ));

        Map<UUID, String> positionNameMap =
                listOfJoPositions.stream()
                        .collect(Collectors.toMap(
                                JobPositionsEntity::getId,
                                jp -> masterPositionNameMap.get(jp.getMasterPositionId()),
                                (existing, replacement) -> existing
                        ));

        for (CandidateApplicationsEntity applicationEntity : applicationsEntities) {
            // commented logic will be used later based on the appropriate data
            //String email = userNameMap.get(positionLocationMap.get(applicationEntity.getPositionId()));
            String email = userEntity.get(0).getEmail();
            Context context = new Context();
            context.setVariable(AppConstants.APPLICATION_NO, applicationEntity.getApplicationNo());
            context.setVariable(AppConstants.POSITION_NAME, positionNameMap.get(applicationEntity.getPositionId()));
            context.setVariable(AppConstants.DEADLINE_DATE, screeningMap.get(applicationEntity.getId()).format(AppConstants.DD_MM_YYYY));
            context.setVariable(AppConstants.CANDIDATE_NAME, commonUtilityProvider.buildFullName(candidateNameMap.get(applicationEntity.getCandidateId())));
            context.setVariable(AppConstants.CANDIDATE_EMAIL, candidateNameMap.get(applicationEntity.getCandidateId()).getEmail());
            context.setVariable(AppConstants.ADDITIONAL_NODE, "Please confirm if any extra documents are required.");
            String htmlBody = templateEngine.process(AppConstants.DISCREPANCY_EMAIL_TEMPLATE, context);
            commonMailService.sendEmailTempleteFile(email, "Candidate updated documents", htmlBody, null);
        }
    }

    private CandidateApplicationDocumentVerificationDTO enrichAdditionalDocIdentifiers(CandidateApplicationDocumentVerificationDTO dto) {
        if (dto.getCandidateDocumentId() == null) {
            dto.setCandidateDocumentId(dto.getId());
        }
        return dto;
    }
}
