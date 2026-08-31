package com.bob.jobportal.service;

import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.entity.*;
import com.bob.db.enums.CandidateApplicationStatus;
import com.bob.db.enums.InterviewSchedulingStatus;
import com.bob.db.enums.ZonalVerificationStatus;
import com.bob.db.mapper.*;
import com.bob.db.repository.*;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.jobportal.model.InterviewerCandidateResponseModel;
import com.bob.jobportal.model.InterviewerPositionResponseModel;
import com.bob.jobportal.model.InterviewerScoreRequestModel;
import com.bob.jobportal.model.InterviewerScoreResponseModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bob.commonutil.exception.ExcelValidationException;
import com.bob.db.dto.CandidateApplicationsDTO;
import com.bob.db.dto.InterviewScheduleDTO;
import com.bob.db.dto.ReservationCategoriesDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InterviewerService {

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;

    @Autowired
    private InterviewPanelMembersRepository interviewPanelMembersRepository;

    @Autowired
    private InterviewPanelsRepository interviewPanelsRepository;

    @Autowired
    private InterviewCommitteeRepository interviewCommitteeRepository;

    @Autowired
    private PositionPanelRepository positionPanelRepository;

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

    @Autowired
    private JobRequisitionsRepository jobRequisitionsRepository;

    @Autowired
    private JobPositionsMapper jobPositionsMapper;

    @Autowired
    private MasterPositionsMapper masterPositionsMapper;

    @Autowired
    private JobRequisitionMapper jobRequisitionMapper;

    @Autowired
    private InterviewScheduleRepository interviewScheduleRepository;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private CandidatesRepository candidatesRepository;

    @Autowired
    private ReservationCategoriesRepository reservationCategoriesRepository;

    @Autowired
    private InterviewCentresRepository interviewCentresRepository;

    @Autowired
    private PanelMembersScoreRepository panelMembersScoreRepository;

    @Autowired
    private CandidateDocumentStoreRepository candidateDocumentStoreRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    @Autowired
    private InterviewScheduleMapper interviewScheduleMapper;

    @Autowired
    private CandidatesMapper candidatesMapper;

    @Autowired
    private CandidateApplicationsMapper candidateApplicationsMapper;

    @Autowired
    private ReservationCategoriesMapper reservationCategoriesMapper;

    @Autowired
    private InterviewCentresMapper interviewCentresMapper;

    @Autowired
    private WorkflowApprovalEntityRepository workflowApprovalEntityRepository;

    @Autowired
    private AuditTrailEntityRepository auditTrailEntityRepository;

    @Autowired
    private InterviewScoreCategoryPassMarksRepository interviewScoreCategoryPassMarksRepository;

    @Autowired
    private CandidateConcessionsRepository candidateConcessionsRepository;

    /**
     * Validates that the user is a member of an Interview committee
     */
    private void validateInterviewCommitteeMembership(UUID userId) {
        // Find all panel member records for this user
        List<InterviewPanelMembersEntity> panelMembers = interviewPanelMembersRepository
                .findAll().stream()
                .filter(member -> member.getPanelMember().getId().equals(userId))
                .toList();

        if (panelMembers.isEmpty()) {
            throw new IllegalStateException("User is not a member of any interview panel");
        }

        // Get all panel IDs
        List<UUID> allPanelIds = panelMembers.stream()
                .map(member -> member.getPanel().getId())
                .distinct()
                .toList();

        // Get all panel details
        List<InterviewPanelsEntity> allPanels = interviewPanelsRepository.findAllById(allPanelIds);

        // Filter panels that have "Interview" committee
        boolean hasInterviewCommittee = allPanels.stream()
                .anyMatch(panel -> panel.getCommittee() != null &&
                        AppConstants.INTERVIEW_COMMITTEE_NAME.equalsIgnoreCase(panel.getCommittee().getCommitteeName()));

        if (!hasInterviewCommittee) {
            String panelNames = allPanels.stream()
                    .map(panel -> String.format("%s (Committee: %s)",
                            panel.getPanelName(),
                            panel.getCommittee() != null ? panel.getCommittee().getCommitteeName() : "N/A"))
                    .collect(Collectors.joining(", "));

            throw new IllegalStateException("User is not part of any interview committee. User panels: " + panelNames);
        }
    }

    @Transactional(readOnly = true)
    public List<InterviewerPositionResponseModel> getInterviewerPositions() {
        // Step 1: Get current user
        UUID userId = securityUtils.getCurrentUserId();
        log.info("Fetching interviewer positions for user: {}", userId);

        // Step 2: Find all panel member records for this user
        List<InterviewPanelMembersEntity> panelMembers = interviewPanelMembersRepository
                .findAll().stream()
                .filter(member -> member.getPanelMember().getId().equals(userId))
                .toList();

        if (panelMembers.isEmpty()) {
            throw new IllegalStateException("User is not a member of any interview panel");
        }

        // Step 3: Get all panel IDs
        List<UUID> allPanelIds = panelMembers.stream()
                .map(member -> member.getPanel().getId())
                .distinct()
                .toList();

        log.info("User belongs to {} panel(s)", allPanelIds.size());

        // Step 4: Get all panel details
        List<InterviewPanelsEntity> allPanels = interviewPanelsRepository.findAllById(allPanelIds);

        // Step 5: Filter panels that have "Interview" committee
        List<InterviewPanelsEntity> interviewPanels = allPanels.stream()
                .filter(panel -> panel.getCommittee() != null &&
                        AppConstants.INTERVIEW_COMMITTEE_NAME.equalsIgnoreCase(panel.getCommittee().getCommitteeName()))
                .toList();

        // Step 6: If no Interview committee panels found, throw error with all panel names
        if (interviewPanels.isEmpty()) {
            String panelNames = allPanels.stream()
                    .map(panel -> String.format("%s (Committee: %s)", 
                            panel.getPanelName(),
                            panel.getCommittee() != null ? panel.getCommittee().getCommitteeName() : "N/A"))
                    .collect(Collectors.joining(", "));
            
            throw new IllegalStateException("User is not part of any interview committee. User panels: " + panelNames);
        }

        log.info("User is part of {} Interview committee panel(s)", interviewPanels.size());

        // Step 7: Get panel IDs for Interview committees
        List<UUID> interviewPanelIds = interviewPanels.stream()
                .map(InterviewPanelsEntity::getId)
                .toList();

        // Step 8: Get all position panels for all Interview panels
        List<PositionPanelEntity> positionPanels = new ArrayList<>();
        for (UUID panelId : interviewPanelIds) {
            positionPanels.addAll(positionPanelRepository.findByInterviewPanel_Id(panelId));
        }

        if (positionPanels.isEmpty()) {
            log.info("No positions assigned to Interview committee panels");
            return Collections.emptyList();
        }

        // Step 9: Extract unique position IDs
        List<UUID> positionIds = positionPanels.stream()
                .map(pp -> pp.getJobPosition().getId())
                .distinct()
                .toList();

        // Step 10: Batch fetch positions
        List<JobPositionsEntity> positions = positionsRepository.findAllByIdIn(positionIds);
        Map<UUID, JobPositionsEntity> positionsMap = positions.stream()
                .collect(Collectors.toMap(JobPositionsEntity::getId, Function.identity()));

        // Step 11: Extract master position IDs and requisition IDs
        List<UUID> masterPositionIds = positions.stream()
                .map(JobPositionsEntity::getMasterPositionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<UUID> requisitionIds = positions.stream()
                .map(JobPositionsEntity::getRequisitionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Step 12: Batch fetch master positions and requisitions
        List<MasterPositionsEntity> masterPositions = masterPositionsRepository.findAllByIdIn(masterPositionIds);
        Map<UUID, MasterPositionsEntity> masterPositionsMap = masterPositions.stream()
                .collect(Collectors.toMap(MasterPositionsEntity::getId, Function.identity()));

        List<JobRequisitionsEntity> requisitions = jobRequisitionsRepository.findAllByIdIn(requisitionIds);
        Map<UUID, JobRequisitionsEntity> requisitionsMap = requisitions.stream()
                .collect(Collectors.toMap(JobRequisitionsEntity::getId, Function.identity()));

        // Step 13: Build response using mappers
        List<InterviewerPositionResponseModel> response = positions.stream()
                .map(position -> {
                    UUID masterPositionId = position.getMasterPositionId();
                    UUID requisitionId = position.getRequisitionId();

                    MasterPositionsEntity masterPosition = masterPositionId != null ? masterPositionsMap.get(masterPositionId) : null;
                    JobRequisitionsEntity requisition = requisitionId != null ? requisitionsMap.get(requisitionId) : null;

                    return InterviewerPositionResponseModel.builder()
                            .position(jobPositionsMapper.toDto(position))
                            .masterPosition(masterPositionsMapper.toDTO(masterPosition))
                            .requisition(jobRequisitionMapper.toDTO(requisition))
                            .build();
                })
                .collect(Collectors.toList());

        log.info("Found {} positions for interviewer", response.size());
        return response;
    }

    @Transactional(readOnly = true)
    public List<InterviewerCandidateResponseModel> getCandidatesForInterview(UUID positionId, LocalDate date) {
        // Step 1: Get current user and find Interview panels
        UUID userId = securityUtils.getCurrentUserId();
        log.info("Fetching candidates for interview - positionId: {}, date: {}, userId: {}", positionId, date, userId);

        List<InterviewPanelMembersEntity> panelMembers = interviewPanelMembersRepository
                .findAll().stream()
                .filter(member -> member.getPanelMember().getId().equals(userId))
                .toList();

        if (panelMembers.isEmpty()) {
            throw new IllegalStateException("User is not a member of any interview panel");
        }

        List<UUID> allPanelIds = panelMembers.stream()
                .map(member -> member.getPanel().getId())
                .distinct()
                .toList();

        List<InterviewPanelsEntity> allPanels = interviewPanelsRepository.findAllById(allPanelIds);

        List<InterviewPanelsEntity> interviewPanels = allPanels.stream()
                .filter(panel -> panel.getCommittee() != null &&
                        AppConstants.INTERVIEW_COMMITTEE_NAME.equalsIgnoreCase(panel.getCommittee().getCommitteeName()))
                .toList();

        if (interviewPanels.isEmpty()) {
            String panelNames = allPanels.stream()
                    .map(panel -> String.format("%s (Committee: %s)",
                            panel.getPanelName(),
                            panel.getCommittee() != null ? panel.getCommittee().getCommitteeName() : "N/A"))
                    .collect(Collectors.joining(", "));

            throw new IllegalStateException("User is not part of any interview committee. User panels: " + panelNames);
        }

        List<UUID> interviewPanelIds = interviewPanels.stream()
                .map(InterviewPanelsEntity::getId)
                .toList();

        // Step 2: Query interview_schedule for given date with Interview panels
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        List<InterviewScheduleEntity> schedules = interviewScheduleRepository.findPanelSchedulings(
                interviewPanelIds,
                startOfDay,
                endOfDay
        );

        // Step 3: Filter by zonal verification status and interview status
        List<InterviewScheduleEntity> verifiedSchedules = schedules.stream()
                .filter(schedule -> schedule.getZonalVerificationStatus() == ZonalVerificationStatus.VERIFIED ||
                        schedule.getZonalVerificationStatus() == ZonalVerificationStatus.PROVISIONALLY_APPROVED ||
                        schedule.getZonalVerificationStatus() == ZonalVerificationStatus.ZONAL_ABSENT ||
                        schedule.getZonalVerificationStatus() == ZonalVerificationStatus.INTERVIEW_ABSENT)
                .filter(schedule -> schedule.getInterviewStatus() != InterviewSchedulingStatus.OFFER_AWAITED && schedule.getInterviewStatus()!=InterviewSchedulingStatus.COMPENSATION)
                .toList();

        if (verifiedSchedules.isEmpty()) {
            log.info("No verified candidates found for date: {}", date);
            return Collections.emptyList();
        }

        // Step 4: Multi-panel assignments are allowed; include candidates across all interview panels.
        // Step 5: Get application IDs and fetch applications
        List<UUID> applicationIds = verifiedSchedules.stream()
                .map(InterviewScheduleEntity::getApplicationId)
                .distinct()
                .toList();

        List<CandidateApplicationsEntity> applications = candidateApplicationsRepository.findAllByIdIn(applicationIds);

        // Step 6: Filter by positionId
        applications = applications.stream()
                .filter(app -> app.getPositionId().equals(positionId))
                .toList();

        if (applications.isEmpty()) {
            log.info("No candidates found for position: {}", positionId);
            return Collections.emptyList();
        }

        Map<UUID, CandidateApplicationsEntity> applicationMap = applications.stream()
                .collect(Collectors.toMap(CandidateApplicationsEntity::getId, Function.identity()));

        // Step 7: Filter schedules to only include applications for this position
        verifiedSchedules = verifiedSchedules.stream()
                .filter(schedule -> applicationMap.containsKey(schedule.getApplicationId()))
                .toList();

        // Step 8: Extract IDs for batch fetching
        List<UUID> candidateIds = applications.stream()
                .map(CandidateApplicationsEntity::getCandidateId)
                .distinct()
                .toList();

        List<UUID> zonalOfficeIds = verifiedSchedules.stream()
                .map(InterviewScheduleEntity::getZonalOfficeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<UUID> scheduleIds = verifiedSchedules.stream()
                .map(InterviewScheduleEntity::getId)
                .toList();

        // Step 9: Batch fetch related entities
        List<CandidatesEntity> candidates = candidatesRepository.findAllByIdIn(candidateIds);
        Map<UUID, CandidatesEntity> candidatesMap = candidates.stream()
                .collect(Collectors.toMap(CandidatesEntity::getId, Function.identity()));

        List<CandidateProfileEntity> profiles = candidateProfileRepository.findAllByCandidateIdIn(candidateIds);
        Map<UUID, CandidateProfileEntity> profilesMap = profiles.stream()
                .collect(Collectors.toMap(CandidateProfileEntity::getCandidateId, Function.identity()));

        List<UUID> categoryIds = profiles.stream()
                .map(CandidateProfileEntity::getReservationCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<ReservationCategoriesEntity> categories = reservationCategoriesRepository.findAllByIdIn(categoryIds);
        Map<UUID, ReservationCategoriesEntity> categoriesMap = categories.stream()
                .collect(Collectors.toMap(ReservationCategoriesEntity::getId, Function.identity()));

        List<InterviewCentresEntity> interviewCentres = interviewCentresRepository.findAllByIdIn(zonalOfficeIds);
        Map<UUID, InterviewCentresEntity> interviewCentresMap = interviewCentres.stream()
                .collect(Collectors.toMap(InterviewCentresEntity::getId, Function.identity()));

        // Step 10: Get panel member scores for this user
        List<PanelMembersScoreEntity> panelScores = panelMembersScoreRepository.findAllByScheduledInterviewIdIn(scheduleIds);

        Map<UUID, PanelMembersScoreEntity> scoresMap = panelScores.stream()
                .filter(score -> score.getPanelMemberId().equals(userId))
                .collect(Collectors.toMap(PanelMembersScoreEntity::getScheduledInterviewId, Function.identity(), (existing, replacement) -> existing));

        // Step 11: Get resume URLs
        DocumentTypesEntity documentTypes = documentTypesRepository.findByDocCode("RESUME");
        final Map<UUID, CandidateDocumentStoreEntity> resumeDocsMap;
        
        if (documentTypes != null) {
            List<CandidateDocumentStoreEntity> documentStoreEntities = 
                    candidateDocumentStoreRepository.findAllByDocumentIdAndCandidateIdIn(documentTypes.getId(), candidateIds);
            resumeDocsMap = documentStoreEntities.stream()
                    .collect(Collectors.toMap(CandidateDocumentStoreEntity::getCandidateId, Function.identity()));
        } else {
            log.warn("RESUME document type not found in document_types table. Resume URLs will not be populated.");
            resumeDocsMap = Collections.emptyMap();
        }

        // Step 12: Build response
        List<InterviewerCandidateResponseModel> response = verifiedSchedules.stream()
                .map(schedule -> {
                    UUID applicationId = schedule.getApplicationId();
                    CandidateApplicationsEntity application = applicationMap.get(applicationId);

                    if (application == null) {
                        return null;
                    }

                    UUID candidateId = application.getCandidateId();
                    UUID zonalOfficeId = schedule.getZonalOfficeId();

                    CandidatesEntity candidate = candidatesMap.get(candidateId);
                    CandidateProfileEntity profile = profilesMap.get(candidateId);
                    UUID categoryId = profile != null ? profile.getReservationCategoryId() : null;
                    ReservationCategoriesEntity category = categoryId != null ? categoriesMap.get(categoryId) : null;
                    InterviewCentresEntity centre = zonalOfficeId != null ? interviewCentresMap.get(zonalOfficeId) : null;
                    PanelMembersScoreEntity score = scoresMap.get(schedule.getId());

                    return InterviewerCandidateResponseModel.builder()
                            .interviewSchedule(interviewScheduleMapper.toDto(schedule))
                            .candidate(candidatesMapper.toDto(candidate))
                            .application(candidateApplicationsMapper.toDTO(application))
                            .category(reservationCategoriesMapper.toDto(category))
                            .interviewCentre(interviewCentresMapper.toDto(centre))
                            .panelScore(score != null ? score.getPanelScore() : null)
                            .panelComments(score != null ? score.getPanelComments() : null)
                            .resumeUrl(resumeDocsMap.get(candidateId) != null ? resumeDocsMap.get(candidateId).getFileUrl() : null)
                            .candidateFullName(profile != null ? commonUtilityProvider.buildFullName(profile) : null)
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("Found {} candidates for interview on {}", response.size(), date);
        return response;
    }

    /**
     * Submit interview score and comments for a candidate
     * After submission, checks if all panel members have scored and calculates final score
     */

    public InterviewerScoreResponseModel submitInterviewScore(InterviewerScoreRequestModel request) {
        log.info("Submitting interview score for candidate: {}, panel: {}", request.getCandidateId(), request.getPanelId());

        UUID currentUserId = securityUtils.getCurrentUserId();
        log.debug("Current user ID: {}", currentUserId);

        // Step 1: Validate user is in Interview committee
        validateInterviewCommitteeMembership(currentUserId);

        // Step 2: Validate panel membership
        List<InterviewPanelMembersEntity> userPanelMemberships = interviewPanelMembersRepository
                .findAllByPanelId(request.getPanelId());

        boolean isUserInPanel = userPanelMemberships.stream()
                .anyMatch(member -> member.getPanelMember().getId().equals(currentUserId));

        if (!isUserInPanel) {
            log.error("User {} is not a member of panel {}", currentUserId, request.getPanelId());
            throw new ResourceNotFoundException("You are not a member of this interview panel");
        }

        // Validate that the scheduled interview is actually assigned to this panel
        // This prevents interviewers from submitting scores for interviews assigned to different panels
        InterviewScheduleEntity scheduledInterview = interviewScheduleRepository
                .findById(request.getScheduledInterviewId())
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled interview not found"));

        if (!scheduledInterview.getPanelId().equals(request.getPanelId())) {
            log.error("Security violation: User {} attempted to submit score for interview {} which is assigned to panel {}, but claimed panel is {}",
                    currentUserId, request.getScheduledInterviewId(), scheduledInterview.getPanelId(), request.getPanelId());
            throw new ResourceNotFoundException("This interview is not assigned to the specified panel");
        }

        // Additional validation: Ensure applicationId, candidateId, and panelId in request match the scheduled interview
        if (!scheduledInterview.getApplicationId().equals(request.getApplicationId())) {
            log.error("Security violation: User {} attempted to submit score with mismatched applicationId. Expected: {}, Provided: {}",
                    currentUserId, scheduledInterview.getApplicationId(), request.getApplicationId());
            throw new IllegalArgumentException("Application ID mismatch");
        }

        if (!scheduledInterview.getCandidateId().equals(request.getCandidateId())) {
            log.error("Security violation: User {} attempted to submit score with mismatched candidateId. Expected: {}, Provided: {}",
                    currentUserId, scheduledInterview.getCandidateId(), request.getCandidateId());
            throw new IllegalArgumentException("Candidate ID mismatch");
        }

        log.info("Validated: User {} is authorized to score interview {} for candidate {} on behalf of panel {}",
                currentUserId, request.getScheduledInterviewId(), request.getCandidateId(), request.getPanelId());

        // Step 3: Check if score already exists for this user (update) or create new
        Optional<PanelMembersScoreEntity> existingScore = panelMembersScoreRepository
                .findByApplicationIdAndScheduledInterviewIdAndCandidateIdAndPanelIdAndPanelMemberId(
                        request.getApplicationId(),
                        request.getScheduledInterviewId(),
                        request.getCandidateId(),
                        request.getPanelId(),
                        currentUserId
                );

        PanelMembersScoreEntity scoreEntity;
        if (existingScore.isPresent()) {
            log.info("Updating existing score for user {} and candidate {}", currentUserId, request.getCandidateId());
            scoreEntity = existingScore.get();
            scoreEntity.setPanelScore(request.getPanelScore());
            scoreEntity.setPanelComments(request.getPanelComments());
            scoreEntity.setInterviewCenterId(request.getInterviewCenterId());
        } else {
            log.info("Creating new score entry for user {} and candidate {}", currentUserId, request.getCandidateId());
            scoreEntity = PanelMembersScoreEntity.builder()
                    .applicationId(request.getApplicationId())
                    .scheduledInterviewId(request.getScheduledInterviewId())
                    .candidateId(request.getCandidateId())
                    .panelId(request.getPanelId())
                    .panelMemberId(currentUserId)
                    .panelScore(request.getPanelScore())
                    .panelComments(request.getPanelComments())
                    .interviewCenterId(request.getInterviewCenterId())
                    .build();
        }

        panelMembersScoreRepository.save(scoreEntity);
        log.info("Score saved successfully for user {} and candidate {}", currentUserId, request.getCandidateId());

        // Update is_absent in candidate_applications table
        CandidateApplicationsEntity application = candidateApplicationsRepository
                .findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        application.setIsAbsent(request.getIsAbsent());

        // Get interview schedule
        InterviewScheduleEntity interviewSchedule = interviewScheduleRepository.findById(request.getScheduledInterviewId())
                .orElseThrow(() -> new ResourceNotFoundException("Interview schedule not found"));

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

        // If marking as absent, update all three statuses to INTERVIEW_ABSENT
        if (request.getIsAbsent()) {
            application.setApplicationStatus(CandidateApplicationStatus.INTERVIEW_ABSENT);

            interviewSchedule.setInterviewStatus(InterviewSchedulingStatus.INTERVIEW_ABSENT);
            interviewSchedule.setZonalVerificationStatus(ZonalVerificationStatus.INTERVIEW_ABSENT);
            interviewSchedule.setFinalScore(null);

            log.info("Set all three statuses to INTERVIEW_ABSENT for application: {}", request.getApplicationId());
        } else if (application.getApplicationStatus() == CandidateApplicationStatus.INTERVIEW_ABSENT
                || interviewSchedule.getInterviewStatus() == InterviewSchedulingStatus.INTERVIEW_ABSENT
                || interviewSchedule.getZonalVerificationStatus() == ZonalVerificationStatus.INTERVIEW_ABSENT) {
            // Only rollback if currently marked as absent - if unmarking absent, rollback application_status from workflow_approval history
            List<WorkflowApprovalEntity> workflowHistory = workflowApprovalEntityRepository
                    .findByEntityTypeAndEntityIdOrderByActionDateDesc(CandidateApplicationsEntity.ENTITY_TYPE, request.getApplicationId());

            if (!workflowHistory.isEmpty()) {
                // Find the first/latest non-INTERVIEW_ABSENT status
                Optional<String> previousStatus = workflowHistory.stream()
                        .map(WorkflowApprovalEntity::getStatus)
                        .filter(status -> !AppConstants.INTERVIEW_ABSENT.equals(status))
                        .findFirst();

                if (previousStatus.isPresent()) {
                    try {
                        CandidateApplicationStatus rollbackStatus = CandidateApplicationStatus.valueOf(previousStatus.get());
                        application.setApplicationStatus(rollbackStatus);
                        log.info("Rolled back application_status to {} for application: {}", rollbackStatus, request.getApplicationId());
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid status value '{}' in workflow history for application: {} - {}",
                                previousStatus.get(), request.getApplicationId(), e.getMessage());
                    }
                } else {
                    log.warn("No non-INTERVIEW_ABSENT status found in workflow history for application: {}", request.getApplicationId());
                }
            } else {
                log.info("No workflow history found for application: {}, not rolling back status", request.getApplicationId());
            }
            
            // Rollback interview_schedule statuses from audit_trail
            List<AuditTrailEntity> auditHistory = auditTrailEntityRepository
                    .findByEntityTypeAndEntityIdOrderByCreatedDateDesc(InterviewScheduleEntity.ENTITY_TYPE, interviewSchedule.getId());
            
            if (!auditHistory.isEmpty()) {
                // Rollback zonalVerificationStatus
                Optional<String> previousZonalStatus = auditHistory.stream()
                        .filter(audit -> "zonalVerificationStatus".equals(audit.getFieldChanged()))
                        .map(AuditTrailEntity::getOldValue)
                        .filter(oldValue -> oldValue != null && !AppConstants.INTERVIEW_ABSENT.equals(oldValue))
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
                    log.warn("No non-INTERVIEW_ABSENT zonal_verification_status found in audit history for schedule: {}", interviewSchedule.getId());
                }
                
                // Rollback interviewStatus
                Optional<String> previousInterviewStatus = auditHistory.stream()
                        .filter(audit -> "interviewStatus".equals(audit.getFieldChanged()))
                        .map(AuditTrailEntity::getOldValue)
                        .filter(oldValue -> oldValue != null && !AppConstants.INTERVIEW_ABSENT.equals(oldValue))
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
                    log.warn("No non-INTERVIEW_ABSENT interview_status found in audit history for schedule: {}", interviewSchedule.getId());
                }
            } else {
                log.info("No audit history found for schedule: {}, not rolling back statuses", interviewSchedule.getId());
            }
        }
        
        // Save with audit trail
        interviewScheduleRepository.saveWithAudit(oldSchedule, interviewSchedule);
        candidateApplicationsRepository.saveWithWorkflow(application);
        log.info("Updated isAbsent status in candidate_applications for application: {}", request.getApplicationId());
        
        // If candidate marked as absent, return appropriate response without scoring logic
        if (application.getIsAbsent()){
            upsertInterviewConcession(request.getApplicationId(), false);
            return InterviewerScoreResponseModel.builder()
                    .message("Candidate marked as absent. Score not evaluated.")
                    .allPanelMembersScored(false)
                    .finalScore(null)
                    .isQualified(false)
                    .build();
        }
        
        // Step 4: Check if all panel members have scored
        List<UUID> allPanelMemberIds = userPanelMemberships.stream()
                .map(member -> member.getPanelMember().getId())
                .distinct()
                .toList();

        log.debug("Total panel members for panel {}: {}", request.getPanelId(), allPanelMemberIds.size());

        List<PanelMembersScoreEntity> allScores = panelMembersScoreRepository
                .findAllByApplicationIdAndScheduledInterviewIdAndCandidateIdAndPanelId(
                        request.getApplicationId(),
                        request.getScheduledInterviewId(),
                        request.getCandidateId(),
                        request.getPanelId()
                );

        Set<UUID> scoredPanelMemberIds = allScores.stream()
                .map(PanelMembersScoreEntity::getPanelMemberId)
                .collect(Collectors.toSet());

        log.debug("Panel members who have scored: {}/{}", scoredPanelMemberIds.size(), allPanelMemberIds.size());

        boolean allMembersScored = scoredPanelMemberIds.containsAll(allPanelMemberIds);

        // Step 5: If all members scored, calculate average and update interview_schedule
        BigDecimal finalScore = null;
        Boolean isQualified = null;

        if (allMembersScored) {
            log.info("All panel members have scored for candidate {}", request.getCandidateId());

            // Calculate average score from all panel members
            List<BigDecimal> scores = allScores.stream()
                    .map(PanelMembersScoreEntity::getPanelScore)
                    .filter(Objects::nonNull)
                    .toList();

            if (!scores.isEmpty()) {
                BigDecimal sum = scores.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                finalScore = sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);

                log.info("Calculated average score: {} from {} scores", finalScore, scores.size());

                // Update interview_schedule with final score and status
                InterviewScheduleEntity scheduleEntity = interviewScheduleRepository
                        .findById(request.getScheduledInterviewId())
                        .orElseThrow(() -> new ResourceNotFoundException("Interview schedule not found"));
                
                // Store old state for audit
                InterviewScheduleEntity oldScheduleForScore = InterviewScheduleEntity.builder()
                        .applicationId(scheduleEntity.getApplicationId())
                        .candidateId(scheduleEntity.getCandidateId())
                        .panelId(scheduleEntity.getPanelId())
                        .interviewStartAt(scheduleEntity.getInterviewStartAt())
                        .interviewEndAt(scheduleEntity.getInterviewEndAt())
                        .interviewDurationMinutes(scheduleEntity.getInterviewDurationMinutes())
                        .meetingLink(scheduleEntity.getMeetingLink())
                        .zonalOfficeId(scheduleEntity.getZonalOfficeId())
                        .finalScore(scheduleEntity.getFinalScore())
                        .interviewStatus(scheduleEntity.getInterviewStatus())
                        .zonalVerificationStatus(scheduleEntity.getZonalVerificationStatus())
                        .zonalSubmitBeforeDate(scheduleEntity.getZonalSubmitBeforeDate())
                        .zonalHrComments(scheduleEntity.getZonalHrComments())
                        .lptRequired(scheduleEntity.getLptRequired())
                        .lptStatus(scheduleEntity.getLptStatus())
                        .mailSendStatus(scheduleEntity.getMailSendStatus())
                        .mailSentOn(scheduleEntity.getMailSentOn())
                        .mailComments(scheduleEntity.getMailComments())
                        .build();
                oldScheduleForScore.setId(scheduleEntity.getId());

                scheduleEntity.setFinalScore(finalScore);

                CandidateProfileEntity candidateProfile = candidateProfileRepository.findByCandidateId(request.getCandidateId())
                        .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

                ReservationCategoriesEntity reservationCategory = candidateProfile.getReservationCategoryId() != null
                        ? reservationCategoriesRepository.findById(candidateProfile.getReservationCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Reservation category not found"))
                        : null;

                int candidateRequiredMarks = getRequiredPassMarksForCategory(candidateProfile.getReservationCategoryId());
                int genRequiredMarks = getGenRequiredPassMarks();

                boolean clearsCandidateThreshold = finalScore.compareTo(BigDecimal.valueOf(candidateRequiredMarks)) >= 0;
                boolean clearsGenThreshold = finalScore.compareTo(BigDecimal.valueOf(genRequiredMarks)) >= 0;
                boolean isGeneralCategory = reservationCategory != null
                        && reservationCategory.getCategoryCode() != null
                        && AppConstants.GENERAL_CATEGORY_CODE.equalsIgnoreCase(reservationCategory.getCategoryCode());

                boolean usedInterviewConcession = !isGeneralCategory && !clearsGenThreshold && clearsCandidateThreshold;
                upsertInterviewConcession(request.getApplicationId(), usedInterviewConcession);

                // Determine interview status based on category-wise pass marks and zonal verification
                if (!clearsCandidateThreshold) {
                    // Score below category-specific qualifying mark - DISQUALIFIED
                    scheduleEntity.setInterviewStatus(InterviewSchedulingStatus.DISQUALIFIED);
                    application.setApplicationStatus(CandidateApplicationStatus.DISQUALIFIED);
                    isQualified = false;
                    log.info("Candidate {} is DISQUALIFIED with score {} (category threshold: {})",
                            request.getCandidateId(), finalScore, candidateRequiredMarks);
                } else {
                    // Score meets candidate-category threshold - check zonal verification
                    if (ZonalVerificationStatus.VERIFIED.equals(scheduleEntity.getZonalVerificationStatus())) {
                        // Zonal verification is VERIFIED - set QUALIFIED
                        scheduleEntity.setInterviewStatus(InterviewSchedulingStatus.QUALIFIED);
                        application.setApplicationStatus(CandidateApplicationStatus.SELECTED);
                        isQualified = true;
                        log.info("Candidate {} is QUALIFIED with score {} (category threshold: {}, concession used: {})",
                                request.getCandidateId(), finalScore, candidateRequiredMarks, usedInterviewConcession);
                    } else if(ZonalVerificationStatus.PROVISIONALLY_APPROVED.equals(scheduleEntity.getZonalVerificationStatus())){
                        scheduleEntity.setInterviewStatus(InterviewSchedulingStatus.PROVISIONALLY_APPROVED);
                    }
                    else {
                        // Zonal verification is not VERIFIED - only set final score, don't change status
                        isQualified = null;
                        log.info("Candidate {} scored {} but zonal verification status is {} - interview_status not updated",
                                request.getCandidateId(), finalScore, scheduleEntity.getZonalVerificationStatus());
                    }
                }

                candidateApplicationsRepository.saveWithWorkflow(application);
                interviewScheduleRepository.saveWithAudit(oldScheduleForScore, scheduleEntity);
                log.info("Interview schedule updated with final score and status");
            }
        }

        // Build response
        String message = allMembersScored
                ? "Score submitted successfully. All panel members have scored and final evaluation is complete."
                : "Score submitted successfully. Waiting for other panel members to complete their evaluation.";

        return InterviewerScoreResponseModel.builder()
                .message(message)
                .allPanelMembersScored(allMembersScored)
                .finalScore(finalScore)
                .isQualified(isQualified)
                .build();
    }

    @Transactional
    public List<InterviewerScoreResponseModel> submitBatchInterviewScores(List<InterviewerScoreRequestModel> requests) {
        log.info("Submitting batch interview scores for {} candidates", requests.size());
        
        List<InterviewerScoreResponseModel> responses = new ArrayList<>();
        
        for (InterviewerScoreRequestModel request : requests) {
            try {
                InterviewerScoreResponseModel response = submitInterviewScore(request);
                responses.add(response);
                log.info("Successfully submitted score for candidate: {}", request.getCandidateId());
            } catch (Exception e) {
                log.error("Failed to submit score for candidate: {} - Error: {}", 
                        request.getCandidateId(), e.getMessage());
                // Add error response
                InterviewerScoreResponseModel errorResponse = InterviewerScoreResponseModel.builder()
                        .message("Error: " + e.getMessage())
                        .allPanelMembersScored(false)
                        .build();
                responses.add(errorResponse);
            }
        }
        
        log.info("Batch score submission completed. Processed {} candidates", responses.size());
        return responses;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Excel Bulk Upload for Interview Scores
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Generates a pre-populated XLSX template for bulk interview score submission.
     * <p>
     * Sheet layout:
     * <ul>
     *   <li>"Interview Scores" (visible) — 4 columns: Candidate (dropdown), Absent (dropdown),
     *       Comments (free text), Score (free text).</li>
     *   <li>"CandidateData" (hidden) — one row per candidate containing the display key and
     *       all UUID fields needed at upload time.</li>
     * </ul>
     * Candidate dropdown label format: {@code AppNo_FullName_CategoryCode_HH:mm-HH:mm}
     */
    @Transactional(readOnly = true)
    public byte[] generateInterviewScoresTemplate(UUID positionId, LocalDate date) throws IOException {
        // Reuse existing method — already validates panel membership, committee, and multi-panel conflict
        List<InterviewerCandidateResponseModel> candidates = getCandidatesForInterview(positionId, date);

        if (candidates.isEmpty()) {
            throw new IllegalStateException(
                    "No candidates are scheduled for interview on " + date + " for this position.");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
        Sheet dataSheet     = workbook.createSheet("Interview Scores");
        Sheet hiddenSheet   = workbook.createSheet("CandidateData");

        // ── Header row on main sheet ─────────────────────────────────────────
        Row header = dataSheet.createRow(0);
        header.createCell(0).setCellValue("Candidate");
        header.createCell(1).setCellValue("Absent");
        header.createCell(2).setCellValue("Comments");
        header.createCell(3).setCellValue("Score");

        // ── Header row on hidden sheet ───────────────────────────────────────
        Row hiddenHeader = hiddenSheet.createRow(0);
        hiddenHeader.createCell(0).setCellValue("CandidateKey");
        hiddenHeader.createCell(1).setCellValue("ScheduledInterviewId");
        hiddenHeader.createCell(2).setCellValue("ApplicationId");
        hiddenHeader.createCell(3).setCellValue("CandidateId");
        hiddenHeader.createCell(4).setCellValue("PanelId");
        hiddenHeader.createCell(5).setCellValue("AppNo");
        hiddenHeader.createCell(6).setCellValue("FullName");

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        int hiddenRowIdx = 1;

        for (InterviewerCandidateResponseModel model : candidates) {
            InterviewScheduleDTO     schedule    = model.getInterviewSchedule();
            CandidateApplicationsDTO application = model.getApplication();
            ReservationCategoriesDTO category    = model.getCategory();

            String appNo    = (application != null && application.getApplicationNo() != null)
                              ? application.getApplicationNo() : "N/A";
            String fullName = (model.getCandidateFullName() != null)
                              ? model.getCandidateFullName() : AppConstants.UNKNOWN;
            String catCode  = (category != null && category.getCategoryCode() != null)
                              ? category.getCategoryCode() : "N/A";
            String timeSlot = "";
            if (schedule != null
                    && schedule.getInterviewStartAt() != null
                    && schedule.getInterviewEndAt()   != null) {
                timeSlot = schedule.getInterviewStartAt().toLocalTime().format(timeFmt)
                         + "-"
                         + schedule.getInterviewEndAt().toLocalTime().format(timeFmt);
            }

            // Visible dropdown label: AppNo_FullName_CategoryCode_HH:mm-HH:mm
            String candidateKey = appNo + "_" + fullName + "_" + catCode + "_" + timeSlot;

            Row hiddenRow = hiddenSheet.createRow(hiddenRowIdx++);
            hiddenRow.createCell(0).setCellValue(candidateKey);
            hiddenRow.createCell(1).setCellValue(schedule != null && schedule.getId() != null
                    ? schedule.getId().toString() : "");
            hiddenRow.createCell(2).setCellValue(application != null && application.getId() != null
                    ? application.getId().toString() : "");
            hiddenRow.createCell(3).setCellValue(schedule != null && schedule.getCandidateId() != null
                    ? schedule.getCandidateId().toString() : "");
            hiddenRow.createCell(4).setCellValue(schedule != null && schedule.getPanelId() != null
                    ? schedule.getPanelId().toString() : "");
            hiddenRow.createCell(5).setCellValue(appNo);
            hiddenRow.createCell(6).setCellValue(fullName);
        }

        int candidateCount = candidates.size();

        // ── Candidate column: dropdown sourced from CandidateData!$A$2:$A$N ─
        DataValidationHelper dvHelper = dataSheet.getDataValidationHelper();

        DataValidationConstraint candidateConstraint = dvHelper.createFormulaListConstraint(
                "CandidateData!$A$2:$A$" + (candidateCount + 1));
        CellRangeAddressList candidateRange   = new CellRangeAddressList(1, candidateCount + 50, 0, 0);
        DataValidation       candidateValidation = dvHelper.createValidation(candidateConstraint, candidateRange);
        candidateValidation.setShowErrorBox(true);
        candidateValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        candidateValidation.createErrorBox("Invalid Selection",
                "Please select a candidate from the dropdown list.");
        dataSheet.addValidationData(candidateValidation);

        // ── Absent column: dropdown — True / False ───────────────────────────
        DataValidationConstraint absentConstraint = dvHelper.createExplicitListConstraint(
                new String[]{"True", "False"});
        CellRangeAddressList absentRange     = new CellRangeAddressList(1, candidateCount + 50, 1, 1);
        DataValidation       absentValidation = dvHelper.createValidation(absentConstraint, absentRange);
        absentValidation.setShowErrorBox(true);
        absentValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        absentValidation.createErrorBox("Invalid Selection", "Please select True or False.");
        dataSheet.addValidationData(absentValidation);

        // ── Hide the CandidateData sheet from the recruiter ──────────────────
        workbook.setSheetHidden(workbook.getSheetIndex(hiddenSheet), true);

        // ── Auto-size all 4 visible columns ─────────────────────────────────
        for (int i = 0; i < 4; i++) {
            dataSheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        log.info("Generated interview scores template with {} candidates for position {} on {}",
                candidateCount, positionId, date);
        return out.toByteArray();
        } // closes try-with-resources (workbook.close() called automatically)
    }

    /**
     * Parses an uploaded XLSX file (produced by {@link #generateInterviewScoresTemplate}),
     * validates every row, and — only if ALL rows pass — submits scores atomically.
     * <p>
     * Validations (all errors collected before throwing):
     * <ul>
     *   <li>Candidate cell must match a key from the hidden CandidateData sheet</li>
     *   <li>No duplicate candidates in the file</li>
     *   <li>Absent field must be "True" or "False"</li>
     *   <li>If Absent=True, Score must be empty</li>
     *   <li>Score must be a number in [0, 100]</li>
     * </ul>
     * Because this method is {@code @Transactional} and calls {@code submitInterviewScore}
     * via {@code this} (bypassing the Spring proxy), all saves share the same transaction.
     * Any unexpected DB error rolls back everything.
     */
    @Transactional
    public List<InterviewerScoreResponseModel> processInterviewScoresFromExcel(MultipartFile file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
        return processInterviewScoresWorkbook(workbook);
        }
    }

    /** Extracted to keep the try-with-resources clean and testable. */
    private List<InterviewerScoreResponseModel> processInterviewScoresWorkbook(Workbook workbook) {
        // ── Sheet presence check ─────────────────────────────────────────────
        Sheet dataSheet = workbook.getSheet("Interview Scores");
        if (dataSheet == null) {
            throw new ExcelValidationException(List.of(
                    "Sheet 'Interview Scores' not found. Please use the template downloaded from this system."));
        }
        Sheet hiddenSheet = workbook.getSheet("CandidateData");
        if (hiddenSheet == null) {
            throw new ExcelValidationException(List.of(
                    "Sheet 'CandidateData' is missing. Please use the template downloaded from this system."));
        }

        // ── Header validation ────────────────────────────────────────────────
        validateInterviewScoresHeader(dataSheet.getRow(0));

        // ── Build lookup map: candidateKey → all IDs + display info ─────────
        record CandidateMeta(UUID scheduledInterviewId, UUID applicationId,
                             UUID candidateId, UUID panelId,
                             String appNo, String fullName) {}

        Map<String, CandidateMeta> candidateLookup = new LinkedHashMap<>();
        for (int i = 1; i <= hiddenSheet.getLastRowNum(); i++) {
            Row row = hiddenSheet.getRow(i);
            if (row == null) continue;
            String key         = readCell(row.getCell(0));
            String scheduleId  = readCell(row.getCell(1));
            String appId       = readCell(row.getCell(2));
            String candidateId = readCell(row.getCell(3));
            String panelId     = readCell(row.getCell(4));
            String appNo       = readCell(row.getCell(5));
            String fullName    = readCell(row.getCell(6));
            if (key == null || scheduleId == null || scheduleId.isBlank()) continue;
            try {
                candidateLookup.put(key, new CandidateMeta(
                        UUID.fromString(scheduleId),
                        UUID.fromString(appId),
                        UUID.fromString(candidateId),
                        UUID.fromString(panelId),
                        appNo != null ? appNo : "N/A",
                        fullName != null ? fullName : AppConstants.UNKNOWN
                ));
            } catch (IllegalArgumentException e) {
                throw new ExcelValidationException(List.of(
                        "Corrupted data in the CandidateData sheet. Please re-download the template."));
            }
        }

        // ── Parse and validate every data row, collecting ALL errors ─────────
        List<String> errors  = new ArrayList<>();
        List<InterviewerScoreRequestModel> requests = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        for (int i = 1; i <= dataSheet.getLastRowNum(); i++) {
            Row row = dataSheet.getRow(i);
            if (row == null) continue;

            String candidateKey = readCell(row.getCell(0));
            if (candidateKey == null || candidateKey.isBlank()) continue; // skip trailing empty rows

            int rowNum = i + 1;

            // Candidate lookup
            CandidateMeta meta = candidateLookup.get(candidateKey.trim());
            if (meta == null) {
                errors.add("Row " + rowNum + ": Unknown candidate '" + candidateKey
                        + "'. Please select from the dropdown in the downloaded template.");
                continue;
            }

            String rowLabel = "Row " + rowNum + " [" + meta.appNo() + " - " + meta.fullName() + "]";

            // Duplicate candidate check
            if (!seenKeys.add(candidateKey.trim())) {
                errors.add(rowLabel + ": Duplicate — this candidate already appears in a previous row.");
                continue;
            }

            // Absent field
            String absentRaw = readCell(row.getCell(1));
            if (absentRaw == null || absentRaw.isBlank()) {
                errors.add(rowLabel + ": 'Absent' is required. Please select True or False.");
                continue;
            }
            boolean isAbsent;
            if ("true".equalsIgnoreCase(absentRaw.trim())) {
                isAbsent = true;
            } else if ("false".equalsIgnoreCase(absentRaw.trim())) {
                isAbsent = false;
            } else {
                errors.add(rowLabel + ": Invalid value for 'Absent': '" + absentRaw
                        + "'. Must be True or False.");
                continue;
            }

            // Comments (optional)
            String comments = readCell(row.getCell(2));

            // Score
            String scoreRaw = readCell(row.getCell(3));
            boolean hasScore = scoreRaw != null && !scoreRaw.isBlank();
            BigDecimal score = null;

            if (isAbsent && hasScore) {
                errors.add(rowLabel + ": Score must be empty when the candidate is marked Absent.");
                continue;
            }
            if (!isAbsent && hasScore) {
                try {
                    score = new BigDecimal(scoreRaw.trim());
                    if (score.scale() > 0 && score.stripTrailingZeros().scale() > 0) {
                        errors.add(rowLabel + ": Score must be a whole number without decimals (provided: " + score + ").");
                        continue;
                    }
                    if (score.compareTo(BigDecimal.ZERO) < 0
                            || score.compareTo(BigDecimal.valueOf(100)) > 0) {
                        errors.add(rowLabel + ": Score must be between 0 and 100 (provided: " + score + ").");
                        continue;
                    }
                } catch (NumberFormatException e) {
                    errors.add(rowLabel + ": Score must be a valid number (provided: '" + scoreRaw + "').");
                    continue;
                }
            }

            requests.add(InterviewerScoreRequestModel.builder()
                    .scheduledInterviewId(meta.scheduledInterviewId())
                    .applicationId(meta.applicationId())
                    .candidateId(meta.candidateId())
                    .panelId(meta.panelId())
                    .panelScore(score)
                    .panelComments(comments)
                    .isAbsent(isAbsent)
                    .interviewCenterId(null)
                    .build());
        }

        if (requests.isEmpty() && errors.isEmpty()) {
            throw new ExcelValidationException(List.of(
                    "No data rows found. Please fill in the candidate scores and re-upload."));
        }

        // All-or-nothing: throw with ALL errors before touching the database
        if (!errors.isEmpty()) {
            List<String> fullErrors = new ArrayList<>();
            fullErrors.add("Nothing has been saved. All rows must pass validation before any data is saved. Please fix the following errors:");
            fullErrors.addAll(errors);
            throw new ExcelValidationException(fullErrors);
        }

        // Submit scores — all within this transaction (self-call bypasses proxy,
        // so submitInterviewScore participates in THIS @Transactional boundary).
        // Any unexpected DB error propagates up and rolls back everything.
        List<InterviewerScoreResponseModel> responses = new ArrayList<>();
        for (InterviewerScoreRequestModel request : requests) {
            responses.add(submitInterviewScore(request));
        }
        log.info("Excel bulk upload: successfully submitted scores for {} candidates", responses.size());
        return responses;
    }

    /** Validates the header row of the uploaded 'Interview Scores' sheet. */
    private void validateInterviewScoresHeader(Row headerRow) {
        if (headerRow == null) {
            throw new ExcelValidationException(List.of(
                    "Header row is missing. Please use the template downloaded from this system."));
        }
        List<String> expected = List.of("Candidate", "Absent", "Comments", "Score");
        List<String> actual   = new ArrayList<>();
        for (int i = 0; i < expected.size(); i++) {
            Cell cell = headerRow.getCell(i);
            actual.add(cell == null ? "" : cell.getStringCellValue().trim());
        }
        if (!actual.equals(expected)) {
            throw new ExcelValidationException(List.of(
                    "Invalid headers. Expected: " + expected + ", Found: " + actual
                    + ". Please use the template downloaded from this system."));
        }
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

    /**
     * Reads any cell as a trimmed String, normalising NUMERIC, BOOLEAN, and FORMULA types.
     * Returns {@code null} for blank or null cells.
     */
    private String readCell(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> {
                String v = cell.getStringCellValue().trim();
                yield v.isEmpty() ? null : v;
            }
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                // Avoid "75.0" for whole numbers
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                // Try numeric first (most formula results are numeric)
                try {
                    double val = cell.getNumericCellValue();
                    if (val == Math.floor(val) && !Double.isInfinite(val)) {
                        yield String.valueOf((long) val);
                    }
                    yield String.valueOf(val);
                } catch (Exception ex) {
                    String s = cell.getStringCellValue().trim();
                    yield s.isEmpty() ? null : s;
                }
            }
            case BLANK   -> null;
            default      -> {
                String s = cell.toString().trim();
                yield s.isEmpty() ? null : s;
            }
        };
    }
}

