package com.bob.jobportal.service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.service.CommonMailService;
import com.bob.commonutil.service.PdfConverterService;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.commonutil.util.CustomMultipartFile;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.InterviewScheduleDTO;
import com.bob.db.enums.*;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.db.entity.*;
import com.bob.db.mapper.*;
import com.bob.db.repository.*;
import com.bob.jobportal.model.BatchAbsentStatusRequest;
import com.bob.jobportal.model.ZonalOverallVerificationRequest;
import com.bob.jobportal.model.ZonalVerificationResponseModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ZonalVerificationService {

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewScheduleRepository interviewScheduleRepository;

    @Autowired
    private CandidatesRepository candidatesRepository;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private InterviewCentresRepository interviewCentresRepository;

    @Autowired
    private ReservationCategoriesRepository reservationCategoriesRepository;

    @Autowired
    private InterviewScoreCategoryPassMarksRepository interviewScoreCategoryPassMarksRepository;

    @Autowired
    private CandidateConcessionsRepository candidateConcessionsRepository;

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

    @Autowired
    private JobRequisitionsRepository jobRequisitionsRepository;

    @Autowired
    private CandidateLocationPreferencesRepository candidateLocationPreferencesRepository;

    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    @Autowired
    private CandidateDocumentStoreRepository candidateDocumentStoreRepository;

    // MapStruct Mappers for entity to DTO conversion
    @Autowired
    private InterviewScheduleMapper interviewScheduleMapper;

    @Autowired
    private CandidatesMapper candidatesMapper;

    @Autowired
    private CandidateApplicationsMapper candidateApplicationsMapper;

    @Autowired
    private InterviewCentresMapper interviewCentresMapper;

    @Autowired
    private ReservationCategoriesMapper reservationCategoriesMapper;

    @Autowired
    private MasterPositionsMapper masterPositionsMapper;

    @Autowired
    private JobRequisitionMapper jobRequisitionMapper;

    @Autowired
    private CommonMailService mailService;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private CandidateApplicationDocumentVerificationRepository candidateApplicationDocumentVerificationRepository;

    @Autowired
    private WorkflowApprovalEntityRepository workflowApprovalEntityRepository;

    @Autowired
    private DepartmentsRepository departmentsRepository;

    @Autowired
    private CandidateAddressRepository candidateAddressRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private AuditTrailEntityRepository auditTrailEntityRepository;

    @Autowired
    private PdfConverterService pdfConverterService;

    @Transactional(readOnly = true)
    public List<ZonalVerificationResponseModel> getCandidatesForZonalVerification(LocalDate interviewDate) {
        // Step 1: Get current user's interview center ID
        UUID userId = securityUtils.getCurrentUserId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getInterviewCenterId() == null) {
            throw new IllegalStateException("User does not have an assigned interview center");
        }

        UUID interviewCenterId = user.getInterviewCenterId();
        log.info("Fetching zonal verification candidates for date: {}, interview center: {}", 
                interviewDate, interviewCenterId);

        // Step 2: Get all interview schedules for the date and center with SCHEDULED, RESCHEDULED, PROVISIONALLY_APPROVED, PENDING, REJECTED or ZONAL_REJECTED status
        List<InterviewSchedulingStatus> statuses = Arrays.asList(
                InterviewSchedulingStatus.SCHEDULED,
                InterviewSchedulingStatus.RESCHEDULED,
                InterviewSchedulingStatus.PROVISIONALLY_APPROVED,
                InterviewSchedulingStatus.ZONAL_REJECTED,
                InterviewSchedulingStatus.PENDING,
                InterviewSchedulingStatus.ZONAL_ABSENT
        );

        List<InterviewScheduleEntity> interviewSchedules = interviewScheduleRepository
                .findByInterviewDateAndZonalOfficeIdAndStatuses(interviewDate, interviewCenterId, statuses);

        if (interviewSchedules.isEmpty()) {
            log.info("No scheduled interviews found for date: {} and center: {}", interviewDate, interviewCenterId);
            return Collections.emptyList();
        }

        // Step 3: Extract all required IDs
        List<UUID> candidateIds = interviewSchedules.stream()
                .map(InterviewScheduleEntity::getCandidateId)
                .distinct()
                .toList();

        List<UUID> applicationIds = interviewSchedules.stream()
                .map(InterviewScheduleEntity::getApplicationId)
                .distinct()
                .toList();

        List<UUID> zonalOfficeIds = interviewSchedules.stream()
                .map(InterviewScheduleEntity::getZonalOfficeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Step 4: Batch fetch all related data (ONE query per table)
        // Fetch candidates
        List<CandidatesEntity> candidates = candidatesRepository.findAllByIdIn(candidateIds);
        Map<UUID, CandidatesEntity> candidatesMap = candidates.stream()
                .collect(Collectors.toMap(CandidatesEntity::getId, Function.identity()));

        // Fetch applications
        List<CandidateApplicationsEntity> applications = candidateApplicationsRepository.findAllByIdIn(applicationIds);
        Map<UUID, CandidateApplicationsEntity> applicationsMap = applications.stream()
                .collect(Collectors.toMap(CandidateApplicationsEntity::getId, Function.identity()));

        // Fetch candidate profiles
        List<CandidateProfileEntity> profiles = candidateProfileRepository.findAllByCandidateIdIn(candidateIds);
        Map<UUID, CandidateProfileEntity> profilesMap = profiles.stream()
                .collect(Collectors.toMap(CandidateProfileEntity::getCandidateId, Function.identity()));

        // Fetch interview centres
        List<InterviewCentresEntity> interviewCentres = interviewCentresRepository.findAllByIdIn(zonalOfficeIds);
        Map<UUID, InterviewCentresEntity> interviewCentresMap = interviewCentres.stream()
                .collect(Collectors.toMap(InterviewCentresEntity::getId, Function.identity()));

        // Fetch all reservation categories (small master data)
        List<ReservationCategoriesEntity> reservationCategories = reservationCategoriesRepository.findAll();
        Map<UUID, ReservationCategoriesEntity> categoriesMap = reservationCategories.stream()
                .collect(Collectors.toMap(ReservationCategoriesEntity::getId, Function.identity()));

        // Fetch job positions using position IDs from applications
        List<UUID> positionIds = applications.stream()
                .map(CandidateApplicationsEntity::getPositionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<JobPositionsEntity> jobPositions = positionsRepository.findAllByIdIn(positionIds);
        Map<UUID, JobPositionsEntity> jobPositionsMap = jobPositions.stream()
                .collect(Collectors.toMap(JobPositionsEntity::getId, Function.identity()));

        List<CandidateLocationPreferenceEntity> locationPreferences = candidateLocationPreferencesRepository
                .findByCandidateAndPosition(candidateIds, positionIds);
        Map<String, CandidateLocationPreferenceEntity> locationPreferenceMap = locationPreferences.stream()
                .collect(Collectors.toMap(
                        curr -> curr.getCandidateId() + "_" + curr.getPositionId(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        // Fetch master positions using master_position_ids from job positions
        List<UUID> masterPositionIds = jobPositions.stream()
                .map(JobPositionsEntity::getMasterPositionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<MasterPositionsEntity> masterPositions = masterPositionsRepository.findAllByIdIn(masterPositionIds);
        Map<UUID, MasterPositionsEntity> masterPositionsMap = masterPositions.stream()
                .collect(Collectors.toMap(MasterPositionsEntity::getId, Function.identity()));

        // Fetch job requisitions using requisition_ids from job positions
        List<UUID> requisitionIds = jobPositions.stream()
                .map(JobPositionsEntity::getRequisitionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<JobRequisitionsEntity> jobRequisitions = jobRequisitionsRepository.findAllByIdIn(requisitionIds);
        Map<UUID, JobRequisitionsEntity> jobRequisitionsMap = jobRequisitions.stream()
                .collect(Collectors.toMap(JobRequisitionsEntity::getId, Function.identity()));

        // Fetch resume URLs for all candidates
        DocumentTypesEntity documentTypes = documentTypesRepository.findByDocCode(DocumentCode.RESUME.toString());
        List<CandidateDocumentStoreEntity> documentStoreEntities = 
                candidateDocumentStoreRepository.findAllByDocumentIdAndCandidateIdIn(documentTypes.getId(), candidateIds);
        Map<UUID, CandidateDocumentStoreEntity> mappedDocumentStore = documentStoreEntities.stream()
                .collect(Collectors.toMap(CandidateDocumentStoreEntity::getCandidateId, Function.identity()));

        // Step 5: Map to response DTOs
        List<ZonalVerificationResponseModel> response = interviewSchedules.stream()
                .map(schedule -> {
                    CandidatesEntity candidate = candidatesMap.get(schedule.getCandidateId());
                    CandidateApplicationsEntity application = applicationsMap.get(schedule.getApplicationId());
                    CandidateProfileEntity profile = profilesMap.get(schedule.getCandidateId());
                    InterviewCentresEntity interviewCentre = interviewCentresMap.get(schedule.getZonalOfficeId());

                    // Get category details
                    UUID categoryId = profile != null ? profile.getReservationCategoryId() : null;
                    ReservationCategoriesEntity category = categoryId != null ? categoriesMap.get(categoryId) : null;

                    // Get position details
                    UUID positionId = application != null ? application.getPositionId() : null;
                    JobPositionsEntity jobPosition = positionId != null ? jobPositionsMap.get(positionId) : null;

                    // Get master position details
                    UUID masterPositionId = jobPosition != null ? jobPosition.getMasterPositionId() : null;
                    MasterPositionsEntity masterPosition = masterPositionId != null ? masterPositionsMap.get(masterPositionId) : null;

                    // Get requisition details
                    UUID requisitionId = jobPosition != null ? jobPosition.getRequisitionId() : null;
                    JobRequisitionsEntity jobRequisition = requisitionId != null ? jobRequisitionsMap.get(requisitionId) : null;

                    // Use mappers to convert entities to DTOs automatically
                    InterviewScheduleDTO scheduleDTO = interviewScheduleMapper.toDto(schedule);

                    Boolean derivedLptRequired = false;
                    String derivedLptStatus = null;
                    if (jobPosition != null && Boolean.TRUE.equals(jobPosition.getIsLocationWise())
                            && Boolean.TRUE.equals(jobPosition.getIsProficientInLocalLanguage())) {
                        derivedLptRequired = true;
                        CandidateLocationPreferenceEntity locationPreference = locationPreferenceMap.get(
                                schedule.getCandidateId() + "_" + positionId
                        );
                        boolean isLocalLanguageStudied = locationPreference != null
                                && Boolean.TRUE.equals(locationPreference.getIsLocalLanguageStudied());
                        derivedLptStatus = isLocalLanguageStudied ? LptStatus.PASS.name() : LptStatus.FAIL.name();
                    }

                    if (scheduleDTO.getLptRequired() == null) {
                        scheduleDTO.setLptRequired(derivedLptRequired);
                    }
                    if (scheduleDTO.getLptStatus() == null || scheduleDTO.getLptStatus().isBlank()) {
                        scheduleDTO.setLptStatus(derivedLptStatus);
                    }
                    
                    // Handle null zonalVerificationStatus - default to PENDING for zonal verification API
                    if (scheduleDTO.getZonalVerificationStatus() == null) {
                        scheduleDTO.setZonalVerificationStatus(ZonalVerificationStatus.PENDING.toString());
                    }
                    
                    return ZonalVerificationResponseModel.builder()
                            .interviewSchedule(scheduleDTO)
                            .candidate(candidatesMapper.toDto(candidate))
                            .application(candidateApplicationsMapper.toDTO(application))
                            .zonalOffice(interviewCentresMapper.toDto(interviewCentre))
                            .category(reservationCategoriesMapper.toDto(category))
                            .categoryId(categoryId != null ? categoryId.toString() : null)
                            .masterPosition(masterPositionsMapper.toDTO(masterPosition))
                            .requisition(jobRequisitionMapper.toDTO(jobRequisition))
                            .resumeUrl(mappedDocumentStore.get(schedule.getCandidateId()) != null ? 
                                    mappedDocumentStore.get(schedule.getCandidateId()).getFileUrl() : null)
                            .candidateFullName(profile != null ? commonUtilityProvider.buildFullName(profile) : null)
                            .build();
                })
                .collect(Collectors.toList());

        log.info("Found {} candidates for zonal verification", response.size());
        return response;
    }

    @Transactional
    public void updateAbsentStatus(UUID applicationId, Boolean isAbsent) {
        log.info("Updating absent status for application: {} to {}", applicationId, isAbsent);
        
        CandidateApplicationsEntity application = candidateApplicationsRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));
        
        application.setIsAbsent(isAbsent);
        
        // Get interview schedule
        InterviewScheduleEntity interviewSchedule = interviewScheduleRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview schedule not found for application: " + applicationId));
        
        // Store old state for audit (manual copy since toBuilder not available)
        InterviewScheduleEntity oldSchedule = InterviewScheduleEntity.builder()
                .applicationId(interviewSchedule.getApplicationId())
                .candidateId(interviewSchedule.getCandidateId())
                .panelId(interviewSchedule.getPanelId())
                .interviewStartAt(interviewSchedule.getInterviewStartAt())
                .interviewEndAt(interviewSchedule.getInterviewEndAt())
                .interviewDurationMinutes(interviewSchedule.getInterviewDurationMinutes())
                .meetingLink(interviewSchedule.getMeetingLink())
                .zonalOfficeId(interviewSchedule.getZonalOfficeId())
                .finalScore(interviewSchedule.getFinalScore())
                .interviewStatus(interviewSchedule.getInterviewStatus())
                .zonalVerificationStatus(interviewSchedule.getZonalVerificationStatus())
                .zonalSubmitBeforeDate(interviewSchedule.getZonalSubmitBeforeDate())
                .zonalHrComments(interviewSchedule.getZonalHrComments())
                .lptRequired(interviewSchedule.getLptRequired())
                .lptStatus(interviewSchedule.getLptStatus())
                .mailSendStatus(interviewSchedule.getMailSendStatus())
                .mailSentOn(interviewSchedule.getMailSentOn())
                .mailComments(interviewSchedule.getMailComments())
                .build();
        oldSchedule.setId(interviewSchedule.getId());
        
        // If marking as absent, update all three statuses to ZONAL_ABSENT
        if (isAbsent) {
            application.setApplicationStatus(CandidateApplicationStatus.ZONAL_ABSENT);
            
            interviewSchedule.setInterviewStatus(InterviewSchedulingStatus.ZONAL_ABSENT);
            interviewSchedule.setZonalVerificationStatus(ZonalVerificationStatus.ZONAL_ABSENT);
            
            log.info("Set all three statuses to ZONAL_ABSENT for application: {}", applicationId);
        } else {
            // If unmarking absent, rollback application_status from workflow_approval history
            List<WorkflowApprovalEntity> workflowHistory = workflowApprovalEntityRepository
                    .findByEntityTypeAndEntityIdOrderByActionDateDesc(CandidateApplicationsEntity.ENTITY_TYPE, applicationId);
            
            if (!workflowHistory.isEmpty()) {
                // Find the first/latest non-ZONAL_ABSENT status
                Optional<String> previousStatus = workflowHistory.stream()
                        .map(WorkflowApprovalEntity::getStatus)
                        .filter(status -> !AppConstants.ZONAL_ABSENT.equals(status))
                        .findFirst();
                
                if (previousStatus.isPresent()) {
                    try {
                        CandidateApplicationStatus rollbackStatus = CandidateApplicationStatus.valueOf(previousStatus.get());
                        application.setApplicationStatus(rollbackStatus);
                        log.info("Rolled back application_status to {} for application: {}", rollbackStatus, applicationId);
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid status value '{}' in workflow history for application: {} - {}", 
                                previousStatus.get(), applicationId, e.getMessage());
                    }
                } else {
                    log.warn("No non-ZONAL_ABSENT status found in workflow history for application: {}", applicationId);
                }
            } else {
                log.info("No workflow history found for application: {}, not rolling back status", applicationId);
            }
            
            // Rollback interview_schedule statuses from audit_trail
            List<AuditTrailEntity> auditHistory = auditTrailEntityRepository
                    .findByEntityTypeAndEntityIdOrderByCreatedDateDesc(InterviewScheduleEntity.ENTITY_TYPE, interviewSchedule.getId());
            
            if (!auditHistory.isEmpty()) {
                // Rollback zonalVerificationStatus
                Optional<String> previousZonalStatus = auditHistory.stream()
                        .filter(audit -> "zonalVerificationStatus".equals(audit.getFieldChanged()))
                        .map(AuditTrailEntity::getOldValue)
                        .filter(oldValue -> oldValue != null && !AppConstants.ZONAL_ABSENT.equals(oldValue))
                        .findFirst();
                
                if (previousZonalStatus.isPresent()) {
                    try {
                        ZonalVerificationStatus rollbackZonalStatus = ZonalVerificationStatus.valueOf(previousZonalStatus.get());
                        interviewSchedule.setZonalVerificationStatus(rollbackZonalStatus);
                        log.info("Rolled back zonal_verification_status to {} for schedule: {}", rollbackZonalStatus, interviewSchedule.getId());
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid zonal_verification_status value '{}' in audit history for schedule: {} - {}", 
                                previousZonalStatus.get(), interviewSchedule.getId(), e.getMessage());
                    }
                } else {
                    log.warn("No non-ZONAL_ABSENT zonal_verification_status found in audit history for schedule: {}", interviewSchedule.getId());
                }
                
                // Rollback interviewStatus
                Optional<String> previousInterviewStatus = auditHistory.stream()
                        .filter(audit -> "interviewStatus".equals(audit.getFieldChanged()))
                        .map(AuditTrailEntity::getOldValue)
                        .filter(oldValue -> oldValue != null && !AppConstants.ZONAL_ABSENT.equals(oldValue))
                        .findFirst();
                
                if (previousInterviewStatus.isPresent()) {
                    try {
                        InterviewSchedulingStatus rollbackInterviewStatus = InterviewSchedulingStatus.valueOf(previousInterviewStatus.get());
                        interviewSchedule.setInterviewStatus(rollbackInterviewStatus);
                        log.info("Rolled back interview_status to {} for schedule: {}", rollbackInterviewStatus, interviewSchedule.getId());
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid interview_status value '{}' in audit history for schedule: {} - {}", 
                                previousInterviewStatus.get(), interviewSchedule.getId(), e.getMessage());
                    }
                } else {
                    log.warn("No non-ZONAL_ABSENT interview_status found in audit history for schedule: {}", interviewSchedule.getId());
                }
            } else {
                log.info("No audit history found for schedule: {}, not rolling back statuses", interviewSchedule.getId());
            }
        }
        
        // Save with audit trail
        interviewScheduleRepository.saveWithAudit(oldSchedule, interviewSchedule);
        candidateApplicationsRepository.saveWithWorkflow(application);
        
        log.info("Successfully updated absent status for application: {}", applicationId);
    }

    @Transactional
    public void updateBatchAbsentStatus(List<BatchAbsentStatusRequest.AbsentStatusUpdate> absentStatusUpdates) {
        log.info("Updating absent status for {} applications", absentStatusUpdates.size());
        
        // Extract all application IDs
        List<UUID> applicationIds = absentStatusUpdates.stream()
                .map(BatchAbsentStatusRequest.AbsentStatusUpdate::getApplicationId)
                .toList();
        
        // Fetch all applications in one query
        List<CandidateApplicationsEntity> applications = candidateApplicationsRepository.findAllById(applicationIds);
        
        // Validate that all applications exist
        if (applications.size() != applicationIds.size()) {
            List<UUID> foundIds = applications.stream()
                    .map(CandidateApplicationsEntity::getId)
                    .toList();
            List<UUID> missingIds = applicationIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            log.error("Applications not found: {}", missingIds);
            throw new ResourceNotFoundException("Applications not found with ids: " + missingIds);
        }
        
        // Create a map for quick lookup
        Map<UUID, CandidateApplicationsEntity> applicationMap = applications.stream()
                .collect(Collectors.toMap(CandidateApplicationsEntity::getId, app -> app));
        
        // Fetch interview schedules for status updates when marking as absent
        List<InterviewScheduleEntity> interviewSchedules = interviewScheduleRepository.findByApplicationIdIn(applicationIds);
        Map<UUID, InterviewScheduleEntity> scheduleMap = interviewSchedules.stream()
                .collect(Collectors.toMap(InterviewScheduleEntity::getApplicationId, schedule -> schedule));
        
        // Create old schedule copies for audit trail
        List<InterviewScheduleEntity> oldSchedules = new ArrayList<>();
        for (InterviewScheduleEntity schedule : interviewSchedules) {
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
        
        // Update isAbsent for each application
        for (BatchAbsentStatusRequest.AbsentStatusUpdate update : absentStatusUpdates) {
            CandidateApplicationsEntity application = applicationMap.get(update.getApplicationId());
            application.setIsAbsent(update.getIsAbsent());
            
            InterviewScheduleEntity schedule = scheduleMap.get(update.getApplicationId());
            
            // If marking as absent, update all three statuses to ZONAL_ABSENT
            if (update.getIsAbsent()) {
                application.setApplicationStatus(CandidateApplicationStatus.ZONAL_ABSENT);
                
                if (schedule != null) {
                    schedule.setInterviewStatus(InterviewSchedulingStatus.ZONAL_ABSENT);
                    schedule.setZonalVerificationStatus(ZonalVerificationStatus.ZONAL_ABSENT);
                }
                log.debug("Set all statuses to ZONAL_ABSENT for application: {}", update.getApplicationId());
            } else {
                // If unmarking absent, rollback application_status from workflow_approval history
                List<WorkflowApprovalEntity> workflowHistory = workflowApprovalEntityRepository
                        .findByEntityTypeAndEntityIdOrderByActionDateDesc(CandidateApplicationsEntity.ENTITY_TYPE, update.getApplicationId());
                
                if (!workflowHistory.isEmpty()) {
                    // Find the first/latest non-ZONAL_ABSENT status
                    Optional<String> previousStatus = workflowHistory.stream()
                            .map(WorkflowApprovalEntity::getStatus)
                            .filter(status -> !AppConstants.ZONAL_ABSENT.equals(status))
                            .findFirst();
                    
                    if (previousStatus.isPresent()) {
                        try {
                            CandidateApplicationStatus rollbackStatus = CandidateApplicationStatus.valueOf(previousStatus.get());
                            application.setApplicationStatus(rollbackStatus);
                            log.debug("Rolled back application_status to {} for application: {}", rollbackStatus, update.getApplicationId());
                        } catch (IllegalArgumentException e) {
                            log.error("Invalid status value '{}' in workflow history for application: {} - {}", 
                                    previousStatus.get(), update.getApplicationId(), e.getMessage());
                        }
                    } else {
                        log.warn("No non-ZONAL_ABSENT status found in workflow history for application: {}", update.getApplicationId());
                    }
                } else {
                    log.debug("No workflow history found for application: {}, not rolling back status", update.getApplicationId());
                }
                
                // Rollback interview_schedule statuses from audit_trail if schedule exists
                if (schedule != null) {
                    List<AuditTrailEntity> auditHistory = auditTrailEntityRepository
                            .findByEntityTypeAndEntityIdOrderByCreatedDateDesc(InterviewScheduleEntity.ENTITY_TYPE, schedule.getId());
                    
                    if (!auditHistory.isEmpty()) {
                        // Rollback zonalVerificationStatus
                        Optional<String> previousZonalStatus = auditHistory.stream()
                                .filter(audit -> "zonalVerificationStatus".equals(audit.getFieldChanged()))
                                .map(AuditTrailEntity::getOldValue)
                                .filter(oldValue -> oldValue != null && !AppConstants.ZONAL_ABSENT.equals(oldValue))
                                .findFirst();
                        
                        if (previousZonalStatus.isPresent()) {
                            try {
                                ZonalVerificationStatus rollbackZonalStatus = ZonalVerificationStatus.valueOf(previousZonalStatus.get());
                                schedule.setZonalVerificationStatus(rollbackZonalStatus);
                                log.debug("Rolled back zonal_verification_status to {} for schedule: {}", rollbackZonalStatus, schedule.getId());
                            } catch (IllegalArgumentException e) {
                                log.error("Invalid zonal_verification_status value '{}' in audit history for schedule: {} - {}", 
                                        previousZonalStatus.get(), schedule.getId(), e.getMessage());
                            }
                        }
                        
                        // Rollback interviewStatus
                        Optional<String> previousInterviewStatus = auditHistory.stream()
                                .filter(audit -> "interviewStatus".equals(audit.getFieldChanged()))
                                .map(AuditTrailEntity::getOldValue)
                                .filter(oldValue -> oldValue != null && !AppConstants.ZONAL_ABSENT.equals(oldValue))
                                .findFirst();
                        
                        if (previousInterviewStatus.isPresent()) {
                            try {
                                InterviewSchedulingStatus rollbackInterviewStatus = InterviewSchedulingStatus.valueOf(previousInterviewStatus.get());
                                schedule.setInterviewStatus(rollbackInterviewStatus);
                                log.debug("Rolled back interview_status to {} for schedule: {}", rollbackInterviewStatus, schedule.getId());
                            } catch (IllegalArgumentException e) {
                                log.error("Invalid interview_status value '{}' in audit history for schedule: {} - {}", 
                                        previousInterviewStatus.get(), schedule.getId(), e.getMessage());
                            }
                        }
                    }
                }
            }
        }
        
        // Save all applications and interview schedules in batch
        candidateApplicationsRepository.saveAllWithWorkflow(applications);
        interviewScheduleRepository.saveAllWithAudit(oldSchedules, interviewSchedules);
        
        log.info("Successfully updated absent status for {} applications", applications.size());
    }

    @Transactional
    public void submitOverallVerification(ZonalOverallVerificationRequest request) {
        log.info("Submitting overall verification - candidateId: {}, applicationId: {}, status: {}", 
                request.getCandidateId(), request.getApplicationId(), request.getZonalVerificationStatus());
        
        // Get current user (Zonal HR)
        UUID zonalHrUserId = securityUtils.getCurrentUserId();
        UserEntity zonalHrUser = userRepository.findById(zonalHrUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Validate user is Zonal HR
        if (!"Zonal_HR".equalsIgnoreCase(zonalHrUser.getRole())) {
            throw new IllegalStateException("Only Zonal HR users can submit overall verification");
        }
        
        // Find interview schedule
        InterviewScheduleEntity interviewSchedule = interviewScheduleRepository.findById(request.getInterviewScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Interview schedule not found with id: " + request.getInterviewScheduleId()));
        
        // Validate application ID and candidate ID match
        if (!interviewSchedule.getApplicationId().equals(request.getApplicationId())) {
            throw new IllegalArgumentException("Application ID mismatch");
        }
        if (!interviewSchedule.getCandidateId().equals(request.getCandidateId())) {
            throw new IllegalArgumentException("Candidate ID mismatch");
        }
        
        // Store old state for audit (manual copy)
        InterviewScheduleEntity oldSchedule = InterviewScheduleEntity.builder()
                .applicationId(interviewSchedule.getApplicationId())
                .candidateId(interviewSchedule.getCandidateId())
                .panelId(interviewSchedule.getPanelId())
                .interviewStartAt(interviewSchedule.getInterviewStartAt())
                .interviewEndAt(interviewSchedule.getInterviewEndAt())
                .interviewDurationMinutes(interviewSchedule.getInterviewDurationMinutes())
                .meetingLink(interviewSchedule.getMeetingLink())
                .zonalOfficeId(interviewSchedule.getZonalOfficeId())
                .finalScore(interviewSchedule.getFinalScore())
                .interviewStatus(interviewSchedule.getInterviewStatus())
                .zonalVerificationStatus(interviewSchedule.getZonalVerificationStatus())
                .zonalSubmitBeforeDate(interviewSchedule.getZonalSubmitBeforeDate())
                .zonalHrComments(interviewSchedule.getZonalHrComments())
                .lptRequired(interviewSchedule.getLptRequired())
                .lptStatus(interviewSchedule.getLptStatus())
                .mailSendStatus(interviewSchedule.getMailSendStatus())
                .mailSentOn(interviewSchedule.getMailSentOn())
                .mailComments(interviewSchedule.getMailComments())
                .build();
        oldSchedule.setId(interviewSchedule.getId());
        
        // Update overall verification fields
        interviewSchedule.setZonalVerificationStatus(request.getZonalVerificationStatus());
        interviewSchedule.setZonalSubmitBeforeDate(request.getZonalSubmitBeforeDate());
        interviewSchedule.setZonalHrComments(request.getZonalHrComments());
        interviewSchedule.setLptRequired(request.getLptRequired());
        interviewSchedule.setLptStatus(request.getLptStatus());
        
        // Fetch candidate application to update application_status
        CandidateApplicationsEntity application = candidateApplicationsRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + request.getApplicationId()));
        
        // Handle application_status and interview_status based on zonal verification status
        if (request.getZonalVerificationStatus() == ZonalVerificationStatus.PROVISIONALLY_APPROVED) {
            // Change A: Set application_status to PROVISIONALLY_APPROVED
            application.setApplicationStatus(CandidateApplicationStatus.PROVISIONALLY_APPROVED);
            // Also set interview_status to PROVISIONALLY_APPROVED
            interviewSchedule.setInterviewStatus(InterviewSchedulingStatus.PROVISIONALLY_APPROVED);
            log.info("Set application_status and interview_status to PROVISIONALLY_APPROVED for application: {}", request.getApplicationId());
        } else if (request.getZonalVerificationStatus() == ZonalVerificationStatus.VERIFIED) {
            CandidateProfileEntity candidateProfile = candidateProfileRepository.findByCandidateId(request.getCandidateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));
            ReservationCategoriesEntity reservationCategory = candidateProfile.getReservationCategoryId() != null
                    ? reservationCategoriesRepository.findById(candidateProfile.getReservationCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation category not found"))
                    : null;

            int candidateRequiredMarks = getRequiredPassMarksForCategory(candidateProfile.getReservationCategoryId());
            int genRequiredMarks = getGenRequiredPassMarks();
            BigDecimal finalScore = interviewSchedule.getFinalScore();

            if (finalScore != null) {
                boolean clearsCandidateThreshold = finalScore.compareTo(BigDecimal.valueOf(candidateRequiredMarks)) >= 0;
                boolean clearsGenThreshold = finalScore.compareTo(BigDecimal.valueOf(genRequiredMarks)) >= 0;
                boolean isGeneralCategory = reservationCategory != null
                        && reservationCategory.getCategoryCode() != null
                        && AppConstants.GENERAL_CATEGORY_CODE.equalsIgnoreCase(reservationCategory.getCategoryCode());
                boolean usedInterviewConcession = !isGeneralCategory && !clearsGenThreshold && clearsCandidateThreshold;

                upsertInterviewConcession(request.getApplicationId(), usedInterviewConcession);

                if (clearsCandidateThreshold) {
                    // Score satisfies category pass mark
                    application.setApplicationStatus(CandidateApplicationStatus.SELECTED);
                    interviewSchedule.setInterviewStatus(InterviewSchedulingStatus.QUALIFIED);
                    log.info("Set application_status to SELECTED and interview_status to QUALIFIED for application: {} (final_score: {}, category threshold: {}, concession used: {})",
                            request.getApplicationId(), finalScore, candidateRequiredMarks, usedInterviewConcession);
                } else {
                    // Score below category threshold
                    application.setApplicationStatus(CandidateApplicationStatus.DISQUALIFIED);
                    interviewSchedule.setInterviewStatus(InterviewSchedulingStatus.DISQUALIFIED);
                    log.info("Final score {} is below category threshold ({}). Set DISQUALIFIED for application {}",
                            finalScore, candidateRequiredMarks, request.getApplicationId());
                }
            } else {
                // Final score doesn't exist - set SCHEDULED and PENDING
                application.setApplicationStatus(CandidateApplicationStatus.SCHEDULED);
                interviewSchedule.setInterviewStatus(InterviewSchedulingStatus.PENDING);
                log.info("Set application_status to SCHEDULED and interview_status to PENDING for application: {} (no final_score yet)",
                        request.getApplicationId());
            }
        } else if (request.getZonalVerificationStatus() == ZonalVerificationStatus.REJECTED) {
            // Change C: Set all three statuses to ZONAL_REJECTED
            application.setApplicationStatus(CandidateApplicationStatus.ZONAL_REJECTED);
            interviewSchedule.setInterviewStatus(InterviewSchedulingStatus.ZONAL_REJECTED);
            interviewSchedule.setZonalVerificationStatus(ZonalVerificationStatus.ZONAL_REJECTED);
            log.info("Set application_status, interview_status and zonal_verification_status to ZONAL_REJECTED for application: {}", request.getApplicationId());
        }
        // For PENDING, don't change application_status
        
        candidateApplicationsRepository.saveWithWorkflow(application);
        
        interviewScheduleRepository.saveWithAudit(oldSchedule, interviewSchedule);
        
        // Send email if status is PROVISIONALLY_APPROVED
        if (request.getZonalVerificationStatus() == ZonalVerificationStatus.PROVISIONALLY_APPROVED) {
            sendZonalHrDiscrepancyEmail(request, interviewSchedule);
        }
        
        log.info("Overall verification submitted successfully for interview schedule: {}", request.getInterviewScheduleId());
    }

    private int getRequiredPassMarksForCategory(UUID reservationCategoryId) {
        if (reservationCategoryId == null) {
            throw new ResourceNotFoundException("Reservation category is not configured for candidate");
        }
        return interviewScoreCategoryPassMarksRepository.findByReservationCategoryId(reservationCategoryId)
                .map(entity -> Integer.valueOf(entity.getRequiredPassMarks()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Interview pass marks are not configured for reservation category: " + reservationCategoryId));
    }

    private int getGenRequiredPassMarks() {
        ReservationCategoriesEntity genCategory = reservationCategoriesRepository
                .findByCategoryCodeIn(List.of(AppConstants.GENERAL_CATEGORY_CODE))
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("GEN reservation category not found"));
        return getRequiredPassMarksForCategory(genCategory.getId());
    }

    private void upsertInterviewConcession(UUID applicationId, boolean usedInterviewConcession) {
        CandidateConcessionsEntity concessions = candidateConcessionsRepository.findByApplicationId(applicationId)
                .orElseGet(() -> CandidateConcessionsEntity.builder()
                        .applicationId(applicationId)
                        .build());
        concessions.setInterviewConcession(usedInterviewConcession);
        candidateConcessionsRepository.save(concessions);
    }

    private void sendZonalHrDiscrepancyEmail(ZonalOverallVerificationRequest request, InterviewScheduleEntity interviewSchedule){
        log.info("Sending Zonal HR discrepancy email for application: {}", request.getApplicationId());
        
        // Fetch rejected documents with their comments
        List<CandidateApplicationDocumentVerificationEntity> rejectedDocs = 
                candidateApplicationDocumentVerificationRepository.findByApplicationId(request.getApplicationId())
                .stream()
                .filter(e -> DocumentZonalVerificationStatus.REJECTED == e.getZonalHrDocStatus())
                .toList();

        Set<UUID> candidateDocumentIds = rejectedDocs.stream()
                .map(CandidateApplicationDocumentVerificationEntity::getCandidateDocumentId)
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
                    String documentName = candidateDocumentStore != null ? documentNamesMap.get(candidateDocumentStore.getDocumentId()) : AppConstants.UNKNOWN_DOCUMENT;
                    
                    Map<String, String> docMap = new HashMap<>();
                    docMap.put(AppConstants.DOCUMENT_NAME, documentName);
                    docMap.put(AppConstants.ZONAL_HR_COMMENTS, doc.getZonalHrDocComments());
                    return docMap;
                })
                .toList();
        
        // Only send email if there are rejected documents
        if (documentList.isEmpty()) {
            log.info("No rejected documents found, skipping email");
            return;
        }
        
        // Validate submit before date
        if (request.getZonalSubmitBeforeDate() == null) {
            throw new CommonException("Submit before date cannot be null for provisionally approved status");
        }
        
        // Fetch candidate details
        CandidatesEntity candidate = candidatesRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        
        CandidateProfileEntity profile = candidateProfileRepository.findByCandidateId(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        CandidateAddressEntity addressEntity = candidateAddressRepository.findByCandidateId(request.getCandidateId())
                .orElseThrow(()->new ResourceNotFoundException("Candidate Address Not found"));

        StateEntity stateEntity = stateRepository.findById(addressEntity.getStateId()).orElseThrow(()->new ResourceNotFoundException("State not found"));
        DistrictEntity districtEntity = districtRepository.findById(addressEntity.getDistrictId()).orElseThrow(()->new ResourceNotFoundException("District Not Found"));



        // Fetch application details
        CandidateApplicationsEntity application = candidateApplicationsRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        
        // Fetch position details
        JobPositionsEntity position = positionsRepository.findById(application.getPositionId())
                .orElseThrow(() -> new ResourceNotFoundException("Position not found"));
        
        MasterPositionsEntity masterPosition = masterPositionsRepository.findById(position.getMasterPositionId())
                .orElseThrow(() -> new ResourceNotFoundException("Master position not found"));

        JobRequisitionsEntity jobRequisition = jobRequisitionsRepository.findById(position.getRequisitionId())
                .orElseThrow(()->new ResourceNotFoundException("Job Requisition not found"));

        DepartmentsEntity department = departmentsRepository.findById(position.getDeptId())
                .orElseThrow(()->new ResourceNotFoundException("Department not found"));
        
        // Build email context
        Context context = new Context();
        context.setVariable(AppConstants.CANDIDATE_NAME, commonUtilityProvider.buildFullName(profile));
        context.setVariable(AppConstants.APPLICATION_NO, application.getApplicationNo());
        context.setVariable(AppConstants.POSITION_NAME, masterPosition.getPositionName());
        context.setVariable(AppConstants.REJECTED_DOCUMENTS, documentList);
        context.setVariable(AppConstants.OVERALL_COMMENTS, request.getZonalHrComments() != null ? request.getZonalHrComments() : "");
        context.setVariable(AppConstants.DEADLINE_DATE, request.getZonalSubmitBeforeDate());
        context.setVariable(AppConstants.COMPANY_NAME, AppConstants.BOB_RECRUITMENT);
        context.setVariable(AppConstants.CANDIDATE_ADDRESS,addressEntity);
        context.setVariable(AppConstants.STATE_NAME,stateEntity.getStateName());
        context.setVariable(AppConstants.DISTRICT_NAME,districtEntity.getDistrictName());
        context.setVariable(AppConstants.APPLICATION_DATE,application.getApplicationDate());
        context.setVariable(AppConstants.CANDIDATE_LAST_NAME,profile.getLastName());
        context.setVariable(AppConstants.ADVERTISEMENT_START_DATE,jobRequisition.getStartDate());
        context.setVariable(AppConstants.DEPARTMENT,department.getDepartmentName());


        // Process template and send email
        String htmlContent = templateEngine.process(AppConstants.ZONAL_HR_EMAIL_TEMPLATE, context);
        String pdfContent = templateEngine.process(AppConstants.ZONAL_HR_DISCREPANCY_EMAIL_ATTACHMENT,context);

        byte[] pdfBytes = pdfConverterService.convertHtmlStringToPdf(pdfContent);
        MultipartFile pdfFile = new CustomMultipartFile(pdfBytes, "file", "zonal-hr-discrepancy-attachment.pdf", "application/pdf");
        mailService.sendEmailTempleteFile(candidate.getEmail(), AppConstants.ZONAL_HR_MESSAGE_SUBJECT, htmlContent, pdfFile);

        log.info("Zonal HR discrepancy email sent successfully to: {}", candidate.getEmail());
    }
}
