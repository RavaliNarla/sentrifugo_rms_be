package com.bob.jobportal.service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.EmbeddingBuilderService;
import com.bob.commonutil.service.FileService;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.entity.*;
import com.bob.db.enums.PositionStatus;
import com.bob.db.enums.RequisitionEditStatus;
import com.bob.db.enums.RequisitionStatus;
import com.bob.db.repository.JobPositionEditRequestRepository;
import com.bob.db.repository.JobPositionsHistoryRepository;
import com.bob.db.repository.JobRequisitionEditRequestRepository;
import com.bob.db.repository.JobRequisitionHistoryRepository;
import com.bob.db.repository.JobRequisitionsRepository;
import com.bob.db.repository.PositionCategoryDistributionHistoryRepository;
import com.bob.db.repository.PositionCategoryNationalDistributionHistoryRepository;
import com.bob.db.repository.PositionStateDistributionHistoryRepository;
import com.bob.db.repository.PositionsRepository;
import com.bob.db.repository.RequisitionApproversRepository;
import com.bob.db.repository.WorkflowApprovalEntityRepository;
import com.bob.db.repository.*;
import com.bob.db.util.DBConstants;
import com.bob.jobportal.config.RequisitionEditFeatureProperties;
import com.bob.jobportal.model.*;
import jakarta.persistence.Column;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RequisitionEditDraftService {

    private static final List<RequisitionEditStatus> ACTIVE_DRAFT_STATUSES = List.of(
            RequisitionEditStatus.DRAFT,
            RequisitionEditStatus.L1_PENDING,
            RequisitionEditStatus.L2_PENDING,
            RequisitionEditStatus.L1_REJECTED,
            RequisitionEditStatus.L2_REJECTED,
            RequisitionEditStatus.APPROVED
    );

    @Autowired
    private JobRequisitionsRepository jobRequisitionsRepository;

    @Autowired
    private JobRequisitionEditRequestRepository jobRequisitionEditRequestRepository;

    @Autowired
    private JobPositionEditRequestRepository jobPositionEditRequestRepository;

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private JobRequisitionHistoryRepository jobRequisitionHistoryRepository;

    @Autowired
    private JobPositionsHistoryRepository jobPositionsHistoryRepository;

    @Autowired
    private PositionStateDistributionHistoryRepository positionStateDistributionHistoryRepository;

    @Autowired
    private PositionCategoryDistributionHistoryRepository positionCategoryDistributionHistoryRepository;

    @Autowired
    private PositionCategoryNationalDistributionHistoryRepository positionCategoryNationalDistributionHistoryRepository;

    @Autowired
    private WorkflowApprovalEntityRepository workflowApprovalEntityRepository;

    @Autowired
    private RequisitionApproversRepository requisitionApproversRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private RequisitionEditFeatureProperties featureProperties;

    @Autowired
    private FileService fileService;

    @Value("${indent.upload.dir}")
    private String indentUploadDir;

    @Autowired
    private EmbeddingBuilderService embeddingBuilderService;

    @Autowired
    private JobPositionExclusionsHistoryRepository jobPositionExclusionsHistoryRepository;

    @Transactional
    public RequisitionEditDraftResponseModel createOrGetDraft(UUID requisitionId, CreateRequisitionEditDraftRequestModel request) {
        ensureFeatureEnabled();

        JobRequisitionsEntity liveRequisition = jobRequisitionsRepository.findById(requisitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found."));

        validateRequisitionEditable(liveRequisition);

        JobRequisitionEditRequestEntity draft = jobRequisitionEditRequestRepository
                .findTopByParentRequisitionIdAndIsActiveTrueAndRequisitionStatusInOrderByCreatedDateDesc(requisitionId, ACTIVE_DRAFT_STATUSES)
                .orElseGet(() -> createDraftFromLive(liveRequisition));

        List<UUID> selectedPositionIds = request != null ? request.getPositionIds() : null;
        ensurePositionDrafts(draft, liveRequisition.getId(), selectedPositionIds);

        // Flush so that the saveAll / deleteAll inside ensurePositionDrafts are
        // visible in the DB before the lazy positionEditRequests collection is loaded
        // inside toResponse(). Hibernate's AUTO flush mode does not detect that custom
        // @SQLDelete UPDATEs affect the upcoming collection SELECT, so we force it here.
        jobPositionEditRequestRepository.flush();

        if (!Boolean.TRUE.equals(liveRequisition.getIsInEditMode())) {
            liveRequisition.setIsInEditMode(true);
            jobRequisitionsRepository.save(liveRequisition);
        }

        log.info("Edit draft ready: requisitionId={}, draftId={}, status={}, baseVersionNo={}, actor={}",
                requisitionId, draft.getId(), draft.getRequisitionStatus(), draft.getBaseVersionNo(),
                safeCurrentUser());

        return toResponse(draft);
    }

    @Transactional(readOnly = true)
    public RequisitionEditDraftResponseModel getCurrentDraft(UUID requisitionId) {
        ensureFeatureEnabled();
        JobRequisitionEditRequestEntity draft = getActiveDraft(requisitionId);
        return toResponse(draft);
    }

    @Transactional
    public RequisitionEditDraftResponseModel updateRequisitionDraft(UUID requisitionId, UpdateRequisitionEditDraftRequestModel request) {
        ensureFeatureEnabled();
        JobRequisitionEditRequestEntity draft = getActiveDraft(requisitionId);
        validateDraftEditable(draft);

        if (request.getRequisitionTitle() != null && !Objects.equals(request.getRequisitionTitle(), draft.getRequisitionTitle())) {
            throw new CommonException("Requisition title cannot be edited.");
        }
        if (request.getStartDate() != null && !Objects.equals(request.getStartDate(), draft.getStartDate())) {
            throw new CommonException("Requisition start date cannot be edited.");
        }
        if (request.getEndDate() != null) {
            if (draft.getStartDate() != null && request.getEndDate().isBefore(draft.getStartDate())) {
                throw new CommonException("Requisition end date cannot be before start date.");
            }
            draft.setEndDate(request.getEndDate());
        }
        if (request.getRequisitionDescription() != null) {
            draft.setRequisitionDescription(request.getRequisitionDescription());
        }
        if (request.getRequisitionComments() != null) {
            draft.setRequisitionComments(request.getRequisitionComments());
        }
        if (request.getIndentPath() != null) {
            draft.setIndentPath(request.getIndentPath());
        }
        if (request.getCutoffDate() != null) {
            draft.setCutoffDate(request.getCutoffDate());
        }

        // A rejected draft that is being revised must re-enter the approval cycle from DRAFT.
        if (draft.getRequisitionStatus() == RequisitionEditStatus.L1_REJECTED
                || draft.getRequisitionStatus() == RequisitionEditStatus.L2_REJECTED) {
            draft.setRequisitionStatus(RequisitionEditStatus.DRAFT);
        }

        return toResponse(jobRequisitionEditRequestRepository.save(draft));
    }

    @Transactional
    public RequisitionEditDraftResponseModel upsertPositionDraft(UUID requisitionId, UUID parentPositionId, UpdatePositionEditDraftRequestModel request) {
        ensureFeatureEnabled();
        JobRequisitionEditRequestEntity draft = getActiveDraft(requisitionId);
        validateDraftEditable(draft);

        JobPositionEditRequestEntity positionDraft = jobPositionEditRequestRepository
                .findByJobEditRequisition_IdAndParentPositionId(draft.getId(), parentPositionId)
                .orElseGet(() -> {
                    JobPositionsEntity livePosition = positionsRepository.findById(parentPositionId)
                            .orElseThrow(() -> new ResourceNotFoundException("Position not found."));
                    if (!Objects.equals(livePosition.getRequisitionId(), requisitionId)) {
                        throw new CommonException("Selected position does not belong to the requisition.");
                    }
                    JobPositionEditRequestEntity cloned = clonePosition(livePosition, draft);
                    draft.getPositionEditRequests().add(cloned);
                    jobRequisitionEditRequestRepository.save(draft);
                    return cloned;
                });

        validateImmutablePositionFields(positionDraft, request);
        applyPositionEditableFields(positionDraft, request);
        applyExclusionsDraftFields(positionDraft, request);
        applyDistributionDraftFields(positionDraft, parentPositionId, request);

        // Editing any position on a rejected draft resets the draft back into DRAFT
        // so it must go through the approval cycle again.
        if (draft.getRequisitionStatus() == RequisitionEditStatus.L1_REJECTED
                || draft.getRequisitionStatus() == RequisitionEditStatus.L2_REJECTED) {
            draft.setRequisitionStatus(RequisitionEditStatus.DRAFT);
            jobRequisitionEditRequestRepository.save(draft);
        }

        jobPositionEditRequestRepository.save(positionDraft);
        return toResponse(getActiveDraft(requisitionId));
    }

    @Transactional
    public RequisitionEditDraftResponseModel upsertPositionDraft(UUID requisitionId,
                                                                 UUID parentPositionId,
                                                                 UpdatePositionEditDraftRequestModel request,
                                                                 MultipartFile indentFile) {
        UpdatePositionEditDraftRequestModel effectiveRequest = request != null ? request : new UpdatePositionEditDraftRequestModel();

        if (indentFile != null && !indentFile.isEmpty()) {
            try {
                String fileName = AppConstants.INDENT_FILE_PREFIX + UUID.randomUUID();
                String uploadedPath = fileService.uploadFile(indentFile, fileName, indentUploadDir);
                effectiveRequest.setIndentPath(indentUploadDir + "/" + uploadedPath);
                effectiveRequest.setIndentName(indentFile.getOriginalFilename());
            } catch (Exception e) {
                throw new CommonException("Error occurred during indent file save");
            }
        }

        return upsertPositionDraft(requisitionId, parentPositionId, effectiveRequest);
    }

    /**
     * Direct approval path that skips L1/L2 and marks the draft APPROVED immediately.
     * This endpoint is kept available so frontend can switch between direct and workflow approval modes.
     */
    @Transactional
    public RequisitionEditDraftResponseModel submitForApprovalDirect(UUID requisitionId, SubmitRequisitionEditDraftRequestModel request) {
        ensureFeatureEnabled();
        log.warn("Direct-mode submit invoked (skips L1/L2): requisitionId={}, actor={}",
                requisitionId, safeCurrentUser());

        JobRequisitionEditRequestEntity draft = getActiveDraft(requisitionId);
        validateDraftEditable(draft);

        draft.setRequisitionStatus(RequisitionEditStatus.APPROVED);
        draft.setApprovedAt(LocalDateTime.now());
        if (request != null && request.getComments() != null) {
            draft.setRequisitionComments(request.getComments());
        }

        JobRequisitionEditRequestEntity saved = jobRequisitionEditRequestRepository.save(draft);
        workflowApprovalEntityRepository.save(createWorkflowEntity(saved));
        log.info("Edit draft direct-approved: draftId={}, requisitionId={}, actor={}",
                saved.getId(), requisitionId, safeCurrentUser());
        return toResponse(saved);
    }

    @Transactional
    public RequisitionEditDraftResponseModel submitForApprovalWorkflow(UUID requisitionId, SubmitRequisitionEditDraftRequestModel request) {
        ensureFeatureEnabled();
        JobRequisitionEditRequestEntity draft = getActiveDraft(requisitionId);
        validateDraftEditable(draft);

        draft.setRequisitionStatus(RequisitionEditStatus.L1_PENDING);
        draft.setSubmittedAt(LocalDateTime.now());
        // Only carry a comment if the recruiter explicitly provides one at submission time.
        // Clearing here prevents stale rejection comments from appearing on the new L1_PENDING row.
        draft.setRequisitionComments(
                (request != null && request.getComments() != null) ? request.getComments() : null);

        JobRequisitionEditRequestEntity saved = jobRequisitionEditRequestRepository.save(draft);
        workflowApprovalEntityRepository.save(createWorkflowEntity(saved));
        log.info("Edit draft submitted for L1 approval: draftId={}, requisitionId={}, actor={}",
                saved.getId(), requisitionId, safeCurrentUser());
        return toResponse(saved);
    }

    @Transactional
    public RequisitionEditDraftResponseModel approveDraftWithWorkflow(UUID requisitionId, ApproveRequisitionEditDraftRequestModel request) {
        ensureFeatureEnabled();
        if (request == null || request.getPostingStatus() == null) {
            throw new CommonException("Posting status is required.");
        }

        JobRequisitionEditRequestEntity draft = getActiveDraft(requisitionId);
        UUID currentUser = securityUtils.getCurrentUserId();
        RequisitionApproversEntity approver = requisitionApproversRepository.findByApproverId(currentUser)
                .orElseThrow(() -> new CommonException("Current user is not configured as requisition approver."));

        RequisitionEditStatus requestedStatus = request.getPostingStatus();
        RequisitionEditStatus targetStatus;
        RequisitionEditStatus requiredCurrentStatus = null;

        if (approver.getApproverRole() == RequisitionApproversEntity.ApproverRole.L1) {
            if (requestedStatus == RequisitionEditStatus.L2_PENDING) {
                targetStatus = RequisitionEditStatus.L2_PENDING;
                requiredCurrentStatus = RequisitionEditStatus.L1_PENDING;
            } else if (requestedStatus == RequisitionEditStatus.L1_REJECTED) {
                targetStatus = RequisitionEditStatus.L1_REJECTED;
            } else {
                throw new CommonException("L1 approver can only set L2_PENDING or L1_REJECTED.");
            }
        } else if (approver.getApproverRole() == RequisitionApproversEntity.ApproverRole.L2) {
            if (requestedStatus == RequisitionEditStatus.APPROVED) {
                targetStatus = RequisitionEditStatus.APPROVED;
                requiredCurrentStatus = RequisitionEditStatus.L2_PENDING;
            } else if (requestedStatus == RequisitionEditStatus.L2_REJECTED) {
                targetStatus = RequisitionEditStatus.L2_REJECTED;
            } else {
                throw new CommonException("L2 approver can only set APPROVED or L2_REJECTED.");
            }
        } else {
            throw new CommonException("Unsupported approver role.");
        }

        if (requiredCurrentStatus != null && draft.getRequisitionStatus() != requiredCurrentStatus) {
            throw new CommonException("Draft is not in required status: " + requiredCurrentStatus);
        }

        draft.setRequisitionStatus(targetStatus);
        if (request.getComments() != null) {
            draft.setRequisitionComments(request.getComments());
        }
        if (targetStatus == RequisitionEditStatus.APPROVED) {
            draft.setApprovedAt(LocalDateTime.now());
        }

        JobRequisitionEditRequestEntity saved = jobRequisitionEditRequestRepository.save(draft);
        workflowApprovalEntityRepository.save(createWorkflowEntity(saved));
        log.info("Edit draft approval action: draftId={}, requisitionId={}, role={}, status={}, actor={}",
                saved.getId(), requisitionId, approver.getApproverRole(), targetStatus, safeCurrentUser());
        if (targetStatus == RequisitionEditStatus.APPROVED
                && approver.getApproverRole() == RequisitionApproversEntity.ApproverRole.L2) {
            // For workflow mode, L2 final approval immediately publishes the draft.
            return publishApprovedDraft(requisitionId);
        }
        return toResponse(saved);
    }

    @Transactional
    public RequisitionEditDraftResponseModel cancelDraft(UUID requisitionId, CancelRequisitionEditDraftRequestModel request) {
        ensureFeatureEnabled();
        JobRequisitionEditRequestEntity draft = getActiveDraft(requisitionId);
        if (draft.getRequisitionStatus() == RequisitionEditStatus.PUBLISHED) {
            throw new CommonException("Published draft cannot be cancelled.");
        }
        draft.setRequisitionStatus(RequisitionEditStatus.CANCELLED);
        if (request != null && request.getComments() != null) {
            draft.setRequisitionComments(request.getComments());
        }
        JobRequisitionEditRequestEntity saved = jobRequisitionEditRequestRepository.save(draft);

        // Release the edit-mode flag on the live requisition so HR can start a fresh draft if needed.
        jobRequisitionsRepository.findById(draft.getParentRequisitionId()).ifPresent(live -> {
            if (Boolean.TRUE.equals(live.getIsInEditMode())) {
                live.setIsInEditMode(false);
                jobRequisitionsRepository.save(live);
            }
        });

        workflowApprovalEntityRepository.save(createWorkflowEntity(saved));
        log.info("Edit draft cancelled: draftId={}, requisitionId={}, actor={}",
                saved.getId(), requisitionId, safeCurrentUser());
        return toResponse(saved);
    }

    @Transactional
    public RequisitionEditDraftResponseModel publishApprovedDraft(UUID requisitionId) {
        ensureFeatureEnabled();
        JobRequisitionEditRequestEntity draft = getActiveDraft(requisitionId);
        if (draft.getRequisitionStatus() != RequisitionEditStatus.APPROVED) {
            throw new CommonException("Only APPROVED draft can be published.");
        }

        // Pessimistic write lock to serialize concurrent publishes/edits on the same requisition.
        JobRequisitionsEntity liveRequisition = jobRequisitionsRepository.findByIdForUpdate(draft.getParentRequisitionId())
                .orElseThrow(() -> new ResourceNotFoundException("Live requisition not found."));

        Integer latestVersion = jobRequisitionHistoryRepository.findLatestVersionNo(liveRequisition.getId());
        int currentVersion = latestVersion == null ? 0 : latestVersion;
        int expectedBaseVersion = currentVersion == 0 ? 1 : currentVersion;
        if (!Objects.equals(draft.getBaseVersionNo(), expectedBaseVersion)) {
            log.warn("Stale draft on publish: draftId={}, requisitionId={}, draftBase={}, currentBase={}",
                    draft.getId(), requisitionId, draft.getBaseVersionNo(), expectedBaseVersion);
            throw new CommonException("Draft base version is stale. Please refresh draft.");
        }
        int publishVersion = currentVersion + 1;
        UUID actorId = securityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        List<JobPositionsEntity> allLivePositions = positionsRepository.findAllByRequisitionIdForUpdate(liveRequisition.getId());
        snapshotLiveState(liveRequisition, allLivePositions, publishVersion, actorId, now);

        applyDraftOnLiveRequisition(liveRequisition, draft);
        Map<UUID, JobPositionsEntity> livePositionMap = allLivePositions.stream()
                .collect(Collectors.toMap(JobPositionsEntity::getId, p -> p));
        for (JobPositionEditRequestEntity draftPosition : draft.getPositionEditRequests()) {
            JobPositionsEntity livePosition = livePositionMap.get(draftPosition.getParentPositionId());
            if (livePosition == null) {
                throw new ResourceNotFoundException("Live position not found for draft position.");
            }
            applyDraftOnLivePosition(livePosition, draftPosition);
        }
        positionsRepository.saveAll(allLivePositions);
        jobRequisitionsRepository.save(liveRequisition);

        draft.setRequisitionStatus(RequisitionEditStatus.PUBLISHED);
        draft.setPublishedAt(now);
        draft.setPublishedBy(actorId);
        jobRequisitionEditRequestRepository.save(draft);
        // Published action has no user-entered comment — store null explicitly.
        workflowApprovalEntityRepository.save(WorkflowApprovalEntity.builder()
                .entityId(draft.getId())
                .entityType(JobRequisitionEditRequestEntity.ENTITY_TYPE)
                .stepNumber(1)
                .approverRole(securityUtils.getCurrentUserRole())
                .approverId(securityUtils.getCurrentUserId())
                .action(DBConstants.WORKFLOW_ACTION_UPDATE)
                .actionDate(now)
                .comments(null)
                .status(RequisitionEditStatus.PUBLISHED.name())
                .build());

        log.info("Edit draft published: draftId={}, requisitionId={}, publishVersion={}, publishedBy={}",
                draft.getId(), requisitionId, publishVersion, actorId);

        try{
            embeddingBuilderService.generateEmbeddingForJobPoitions(List.of(requisitionId));
        } catch (Exception e) {
            log.info("Failed to generate embeddings after requisition edit publish: requisitionId={}, error={}", requisitionId, e.getMessage());
        }
        return toResponse(draft);
    }

    private void ensureFeatureEnabled() {
        if (!featureProperties.isEnabled()) {
            throw new CommonException("Requisition Edit feature is currently disabled.");
        }
    }

    private String safeCurrentUser() {
        try {
            UUID id = securityUtils.getCurrentUserId();
            return id != null ? id.toString() : "anonymous";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private JobRequisitionEditRequestEntity getActiveDraft(UUID requisitionId) {
        return jobRequisitionEditRequestRepository
                .findTopByParentRequisitionIdAndIsActiveTrueAndRequisitionStatusInOrderByCreatedDateDesc(requisitionId, ACTIVE_DRAFT_STATUSES)
                .orElseThrow(() -> new ResourceNotFoundException("No active draft found for requisition."));
    }

    private void validateDraftEditable(JobRequisitionEditRequestEntity draft) {
        if (draft.getRequisitionStatus() != RequisitionEditStatus.DRAFT
                && draft.getRequisitionStatus() != RequisitionEditStatus.L1_REJECTED
                && draft.getRequisitionStatus() != RequisitionEditStatus.L2_REJECTED) {
            throw new CommonException("Draft is not editable in current status: " + draft.getRequisitionStatus());
        }
    }

    private void validateRequisitionEditable(JobRequisitionsEntity requisition) {
        if (requisition.getRequisitionStatus() != RequisitionStatus.APPROVED) {
            throw new CommonException("Only APPROVED requisitions can be edited.");
        }
        LocalDate today = LocalDate.now();
        if (requisition.getEndDate() != null && requisition.getEndDate().isBefore(today)) {
            throw new CommonException("Requisition edit is not allowed after end date.");
        }
    }

    private JobRequisitionEditRequestEntity createDraftFromLive(JobRequisitionsEntity liveRequisition) {
        Integer latestVersion = jobRequisitionHistoryRepository.findLatestVersionNo(liveRequisition.getId());
        int baseVersion = (latestVersion == null || latestVersion <= 0) ? 1 : latestVersion;

        JobRequisitionEditRequestEntity draft = JobRequisitionEditRequestEntity.builder()
                .parentRequisitionId(liveRequisition.getId())
                .requestType("EDIT")
                .requisitionTitle(liveRequisition.getRequisitionTitle())
                .requisitionDescription(liveRequisition.getRequisitionDescription())
                .startDate(liveRequisition.getStartDate())
                .endDate(liveRequisition.getEndDate())
                .requisitionStatus(RequisitionEditStatus.DRAFT)
                .requisitionComments(null)
                .indentPath(null)
                .cutoffDate(liveRequisition.getCutoffDate())
                .baseVersionNo(baseVersion)
                .build();
        return jobRequisitionEditRequestRepository.save(draft);
    }

    private void ensurePositionDrafts(JobRequisitionEditRequestEntity draft, UUID requisitionId, List<UUID> selectedPositionIds) {
        // Both null (body absent / {}) and empty list mean "zero positions" — always reconcile.
        // Effectively: whatever the caller sends is the desired complete set.
        List<UUID> ids = selectedPositionIds != null ? selectedPositionIds : Collections.emptyList();

        List<JobPositionsEntity> livePositions = ids.isEmpty()
                ? Collections.emptyList()
                : positionsRepository.findAllByIdIn(ids);

        if (!ids.isEmpty() && livePositions.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more selected positions were not found.");
        }

        for (JobPositionsEntity livePosition : livePositions) {
            if (!Objects.equals(livePosition.getRequisitionId(), requisitionId)) {
                throw new CommonException("Selected position does not belong to the requisition.");
            }
        }

        // Reconcile the draft position map to exactly match the provided set:
        // remove positions no longer selected, add positions newly selected.
        Set<UUID> incomingIds = new HashSet<>(ids);
        List<JobPositionEditRequestEntity> existingDrafts =
                jobPositionEditRequestRepository.findAllByJobEditRequisition_Id(draft.getId());

        // Remove deselected positions (soft-delete via @SQLDelete).
        List<JobPositionEditRequestEntity> toDelete = existingDrafts.stream()
                .filter(e -> !incomingIds.contains(e.getParentPositionId()))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            jobPositionEditRequestRepository.deleteAll(toDelete);
        }

        // Add newly selected positions that don't have an active draft yet.
        // Before inserting, hard-delete any pre-existing inactive row with the same key
        // so that the unique constraint on (draft_id, parent_position_id) is not violated.
        // Save directly via the position repo (avoids cascade/L1-cache interference with soft-deletes).
        Set<UUID> alreadyDrafted = existingDrafts.stream()
                .filter(e -> incomingIds.contains(e.getParentPositionId()))
                .map(JobPositionEditRequestEntity::getParentPositionId)
                .collect(Collectors.toSet());

        List<JobPositionEditRequestEntity> toAdd = new ArrayList<>();
        for (JobPositionsEntity livePosition : livePositions) {
            if (!alreadyDrafted.contains(livePosition.getId())) {
                // Cascade hard-delete of any inactive rows (FK order: children first, then parent).
                jobPositionEditRequestRepository.hardDeleteInactiveCategoryDistribsByDraftAndParentPosition(
                        draft.getId(), livePosition.getId());
                jobPositionEditRequestRepository.hardDeleteInactiveStateDistribsByDraftAndParentPosition(
                        draft.getId(), livePosition.getId());
                jobPositionEditRequestRepository.hardDeleteInactiveNationalDistribsByDraftAndParentPosition(
                        draft.getId(), livePosition.getId());
                jobPositionEditRequestRepository.hardDeleteInactiveByDraftAndParentPosition(
                        draft.getId(), livePosition.getId());
                toAdd.add(clonePosition(livePosition, draft));
            }
        }
        if (!toAdd.isEmpty()) {
            jobPositionEditRequestRepository.saveAll(toAdd);
        }
    }

    private JobPositionEditRequestEntity clonePosition(JobPositionsEntity source, JobRequisitionEditRequestEntity draft) {
        JobPositionEditRequestEntity target = JobPositionEditRequestEntity.builder()
                .jobEditRequisition(draft)
                .parentPositionId(source.getId())
                .masterPositionId(source.getMasterPositionId())
                .deptId(source.getDeptId())
                .totalVacancies(source.getTotalVacancies())
                .eligibilityAgeMin(source.getEligibilityAgeMin())
                .eligibilityAgeMax(source.getEligibilityAgeMax())
                .employmentType(source.getEmploymentType())
                .cibilScore(source.getCibilScore())
                .gradeId(source.getGradeId())
                .mandatoryEducation(source.getMandatoryEducation())
                .preferredEducation(source.getPreferredEducation())
                .mandatoryExperience(source.getMandatoryExperience())
                .preferredExperience(source.getPreferredExperience())
                .rolesResponsibilities(source.getRolesResponsibilities())
                .positionStatus(source.getPositionStatus())
                .isLocationPreferenceEnabled(source.getIsLocationPreferenceEnabled())
                .isLocationWise(source.getIsLocationWise())
                .totalExperience(source.getTotalExperience())
                .contractYears(source.getContractYears())
                .mandatoryExperienceMonths(source.getMandatoryExperienceMonths())
                .preferredExperienceMonths(source.getPreferredExperienceMonths())
                .indentPath(source.getIndentPath())
                .approvedBy(source.getApprovedBy())
                .approvedOn(source.getApprovedOn())
                .isMedicalRequired(source.getIsMedicalRequired())
                .mandatoryEduRulesJson(source.getMandatoryEduRulesJson())
                .preferredEduRulesJson(source.getPreferredEduRulesJson())
                .indentName(source.getIndentName())
                .indentOthers(source.getIndentOthers())
                .isMandatoryExpMonthsEduWise(source.getIsMandatoryExpMonthsEduWise())
                .isPreferredExpMonthsEduWise(source.getIsPreferredExpMonthsEduWise())
                .mandatoryExpMonthsEduWise(source.getMandatoryExpMonthsEduWise())
                .preferredExpMonthsEduWise(source.getPreferredExpMonthsEduWise())
                .isProficientInLocalLanguage(source.getIsProficientInLocalLanguage())
                .isAgeRelWdsWomen(source.getIsAgeRelWdsWomen())
                .isAgeRelRiotVictimFamily(source.getIsAgeRelRiotVictimFamily())
                .isIntermediateRequired(source.getIsIntermediateRequired())
                .dynamicFields(source.getDynamicFields())
                .build();

        if (source.getPositionStateDistributions() != null) {
            for (PositionStateDistributionEntity sourceState : source.getPositionStateDistributions()) {
                PositionStateDistributionEditRequestEntity stateDraft = PositionStateDistributionEditRequestEntity.builder()
                        .jobEditPosition(target)
                        .stateId(sourceState.getStateId())
                        .cityId(sourceState.getCityId())
                        .totalVacancies(sourceState.getTotalVacancies())
                        .localLanguage(sourceState.getLocalLanguage())
                        .build();

                if (sourceState.getPositionCategoryDistributions() != null) {
                    for (PositionCategoryDistributionEntity sourceCategory : sourceState.getPositionCategoryDistributions()) {
                        PositionCategoryDistributionEditRequestEntity categoryDraft = PositionCategoryDistributionEditRequestEntity.builder()
                                .jobEditPosition(target)
                                .stateDistribution(stateDraft)
                                .reservationCategoryId(sourceCategory.getReservationCategoryId())
                                .disabilityCategoryId(sourceCategory.getDisabilityCategoryId())
                                .vacancyCount(sourceCategory.getVacancyCount())
                                .isDisability(sourceCategory.getIsDisability())
                                .build();
                        stateDraft.getPositionCategoryDistributionEditRequests().add(categoryDraft);
                    }
                }
                target.getPositionStateDistributionEditRequests().add(stateDraft);
            }
        }

        if (source.getPositionCategoryNationalDistributions() != null) {
            for (PositionCategoryNationalDistributionEntity sourceNational : source.getPositionCategoryNationalDistributions()) {
                PositionCategoryNationalDistributionEditRequestEntity nationalDraft = PositionCategoryNationalDistributionEditRequestEntity.builder()
                        .jobEditPosition(target)
                        .reservationCategoryId(sourceNational.getReservationCategoryId())
                        .disabilityCategoryId(sourceNational.getDisabilityCategoryId())
                        .vacancyCount(sourceNational.getVacancyCount())
                        .isDisability(sourceNational.getIsDisability())
                        .build();
                target.getPositionCategoryNationalDistributionEditRequests().add(nationalDraft);
            }
        }

        if(source.getJobPositionExclusion() !=null){
            for (JobPositionExclusionsEntity sourceExclusion : source.getJobPositionExclusion()) {
                JobPositionExclusionsEditRequestEntity exclusionDraft = JobPositionExclusionsEditRequestEntity.builder()
                        .jobEditPosition(target)
                        .exclusionId(sourceExclusion.getExclusionId())
                        .isExcluded(sourceExclusion.getIsExcluded())
                        .build();
                target.getJobPositionExclusionsEditRequestEntity().add(exclusionDraft);
            }
        }

        return target;
    }

    private void snapshotLiveState(JobRequisitionsEntity requisition,
                                   List<JobPositionsEntity> positions,
                                   int versionNo,
                                   UUID actorId,
                                   LocalDateTime changedAt) {

        JobRequisitionHistoryEntity reqHistory = JobRequisitionHistoryEntity.builder()
                .requisitionId(requisition.getId())
                .versionNo(versionNo)
                .actionType("UPDATE")
                .requisitionTitle(requisition.getRequisitionTitle())
                .requisitionDescription(requisition.getRequisitionDescription())
                .startDate(requisition.getStartDate())
                .endDate(requisition.getEndDate())
                .requisitionStatus(requisition.getRequisitionStatus() != null ? requisition.getRequisitionStatus().name() : null)
                .requisitionComments(requisition.getRequisitionComments())
                .indentPath(null)
                .cutoffDate(requisition.getCutoffDate())
                .changedBy(actorId)
                .changedDate(changedAt)
                .isActive(requisition.getIsActive())
                .build();
        jobRequisitionHistoryRepository.save(reqHistory);

        List<JobPositionsHistoryEntity> positionHistories = new ArrayList<>();
        List<PositionStateDistributionHistoryEntity> stateHistories = new ArrayList<>();
        List<PositionCategoryDistributionHistoryEntity> categoryHistories = new ArrayList<>();
        List<PositionCategoryNationalDistributionHistoryEntity> nationalHistories = new ArrayList<>();
        List<JobPositionExclusionsHistoryEntity> exclusionHistories = new ArrayList<>();

        for (JobPositionsEntity p : positions) {
            positionHistories.add(JobPositionsHistoryEntity.builder()
                    .jobPositionId(p.getId())
                    .versionNo(versionNo)
                    .actionType("UPDATE")
                    .changedBy(actorId)
                    .changedDate(changedAt)
                    .requisitionId(p.getRequisitionId())
                    .masterPositionId(p.getMasterPositionId())
                    .deptId(p.getDeptId())
                    .totalVacancies(p.getTotalVacancies())
                    .eligibilityAgeMin(p.getEligibilityAgeMin())
                    .eligibilityAgeMax(p.getEligibilityAgeMax())
                    .employmentType(p.getEmploymentType())
                    .cibilScore(p.getCibilScore())
                    .gradeId(p.getGradeId())
                    .mandatoryEducation(p.getMandatoryEducation())
                    .preferredEducation(p.getPreferredEducation())
                    .mandatoryExperience(p.getMandatoryExperience())
                    .preferredExperience(p.getPreferredExperience())
                    .rolesResponsibilities(p.getRolesResponsibilities())
                    .positionStatus(p.getPositionStatus() != null ? p.getPositionStatus().name() : null)
                    .isLocationPreferenceEnabled(p.getIsLocationPreferenceEnabled())
                    .isLocationWise(p.getIsLocationWise())
                    .totalExperience(p.getTotalExperience())
                    .contractYears(p.getContractYears())
                    .mandatoryExperienceMonths(p.getMandatoryExperienceMonths())
                    .preferredExperienceMonths(p.getPreferredExperienceMonths())
                    .indentPath(p.getIndentPath())
                    .approvedBy(p.getApprovedBy())
                    .approvedOn(p.getApprovedOn())
                    .isMedicalRequired(p.getIsMedicalRequired())
                    .mandatoryEduRulesJson(p.getMandatoryEduRulesJson())
                    .preferredEduRulesJson(p.getPreferredEduRulesJson())
                    .indentName(p.getIndentName())
                    .indentOthers(p.getIndentOthers())
                    .cutoffDate(p.getCutoffDate())
                    .isMandatoryExpMonthsEduWise(p.getIsMandatoryExpMonthsEduWise())
                    .isPreferredExpMonthsEduWise(p.getIsPreferredExpMonthsEduWise())
                    .mandatoryExpMonthsEduWise(p.getMandatoryExpMonthsEduWise())
                    .preferredExpMonthsEduWise(p.getPreferredExpMonthsEduWise())
                    .isProficientInLocalLanguage(p.getIsProficientInLocalLanguage())
                    .isAgeRelWdsWomen(p.getIsAgeRelWdsWomen())
                    .isAgeRelRiotVictimFamily(p.getIsAgeRelRiotVictimFamily())
                    .isIntermediateRequired(p.getIsIntermediateRequired())
                    .isActive(p.getIsActive())
                    .dynamicFields(p.getDynamicFields())
                    .build());

            if (p.getPositionStateDistributions() != null) {
                for (PositionStateDistributionEntity s : p.getPositionStateDistributions()) {
                    stateHistories.add(PositionStateDistributionHistoryEntity.builder()
                            .positionStateDistId(s.getId())
                            .versionNo(versionNo)
                            .actionType("UPDATE")
                            .changedBy(actorId)
                            .changedDate(changedAt)
                            .positionId(p.getId())
                            .stateId(s.getStateId())
                            .cityId(s.getCityId())
                            .totalVacancies(s.getTotalVacancies())
                            .localLanguage(s.getLocalLanguage())
                            .isActive(s.getIsActive())
                            .build());

                    if (s.getPositionCategoryDistributions() != null) {
                        for (PositionCategoryDistributionEntity c : s.getPositionCategoryDistributions()) {
                            categoryHistories.add(PositionCategoryDistributionHistoryEntity.builder()
                                    .positionCategoryDistId(c.getId())
                                    .versionNo(versionNo)
                                    .actionType("UPDATE")
                                    .changedBy(actorId)
                                    .changedDate(changedAt)
                                    .positionId(p.getId())
                                    .stateDistributionId(s.getId())
                                    .reservationCategoryId(c.getReservationCategoryId())
                                    .disabilityCategoryId(c.getDisabilityCategoryId())
                                    .vacancyCount(c.getVacancyCount())
                                    .isDisability(c.getIsDisability())
                                    .isActive(c.getIsActive())
                                    .build());
                        }
                    }
                }
            }

            if (p.getPositionCategoryNationalDistributions() != null) {
                for (PositionCategoryNationalDistributionEntity c : p.getPositionCategoryNationalDistributions()) {
                    nationalHistories.add(PositionCategoryNationalDistributionHistoryEntity.builder()
                            .positionCatNatDistId(c.getId())
                            .versionNo(versionNo)
                            .actionType("UPDATE")
                            .changedBy(actorId)
                            .changedDate(changedAt)
                            .positionId(p.getId())
                            .reservationCategoryId(c.getReservationCategoryId())
                            .disabilityCategoryId(c.getDisabilityCategoryId())
                            .vacancyCount(c.getVacancyCount())
                            .isDisability(c.getIsDisability())
                            .isActive(c.getIsActive())
                            .build());
                }
            }
            if(p.getJobPositionExclusion() != null){
                for (JobPositionExclusionsEntity e : p.getJobPositionExclusion()) {
                    exclusionHistories.add(JobPositionExclusionsHistoryEntity.builder()
                            .jobPositionExclusionsId(e.getId())
                            .versionNo(versionNo)
                            .actionType("UPDATE")
                            .positionId(e.getJobPosition().getId())
                            .isActive(e.getIsActive())
                            .exclusionId(e.getExclusionId())
                            .isExcluded(e.getIsExcluded())
                            .build());
                }
            }
        }

        if (!positionHistories.isEmpty()) jobPositionsHistoryRepository.saveAll(positionHistories);
        if (!stateHistories.isEmpty()) positionStateDistributionHistoryRepository.saveAll(stateHistories);
        if (!categoryHistories.isEmpty()) positionCategoryDistributionHistoryRepository.saveAll(categoryHistories);
        if (!nationalHistories.isEmpty()) positionCategoryNationalDistributionHistoryRepository.saveAll(nationalHistories);
        if (!exclusionHistories.isEmpty()) jobPositionExclusionsHistoryRepository.saveAll(exclusionHistories);
    }

    private void applyDraftOnLiveRequisition(JobRequisitionsEntity liveRequisition, JobRequisitionEditRequestEntity draft) {
        liveRequisition.setRequisitionDescription(draft.getRequisitionDescription());
        liveRequisition.setEndDate(draft.getEndDate());
        liveRequisition.setRequisitionComments(draft.getRequisitionComments());
        liveRequisition.setCutoffDate(draft.getCutoffDate());
        liveRequisition.setRequisitionStatus(RequisitionStatus.APPROVED);
        liveRequisition.setIsInEditMode(false);
    }

    private void applyDraftOnLivePosition(JobPositionsEntity live, JobPositionEditRequestEntity draft) {
        live.setMasterPositionId(draft.getMasterPositionId());
        live.setDeptId(draft.getDeptId());
        live.setTotalVacancies(draft.getTotalVacancies());
        live.setEligibilityAgeMin(draft.getEligibilityAgeMin());
        live.setEligibilityAgeMax(draft.getEligibilityAgeMax());
        live.setEmploymentType(draft.getEmploymentType());
        live.setCibilScore(draft.getCibilScore());
        live.setGradeId(draft.getGradeId());
        live.setMandatoryEducation(draft.getMandatoryEducation());
        live.setPreferredEducation(draft.getPreferredEducation());
        live.setMandatoryExperience(draft.getMandatoryExperience());
        live.setPreferredExperience(draft.getPreferredExperience());
        live.setRolesResponsibilities(draft.getRolesResponsibilities());
        live.setContractYears(draft.getContractYears());
        live.setIsLocationPreferenceEnabled(draft.getIsLocationPreferenceEnabled());
        live.setIsLocationWise(draft.getIsLocationWise());
        live.setTotalExperience(draft.getTotalExperience());
        live.setMandatoryExperienceMonths(draft.getMandatoryExperienceMonths());
        live.setPreferredExperienceMonths(draft.getPreferredExperienceMonths());
        live.setIndentPath(draft.getIndentPath());
        live.setApprovedBy(draft.getApprovedBy());
        live.setApprovedOn(draft.getApprovedOn());
        live.setIsMedicalRequired(draft.getIsMedicalRequired());
        live.setMandatoryEduRulesJson(draft.getMandatoryEduRulesJson());
        live.setPreferredEduRulesJson(draft.getPreferredEduRulesJson());
        live.setIndentName(draft.getIndentName());
        live.setIndentOthers(draft.getIndentOthers());
        live.setIsMandatoryExpMonthsEduWise(draft.getIsMandatoryExpMonthsEduWise());
        live.setIsPreferredExpMonthsEduWise(draft.getIsPreferredExpMonthsEduWise());
        live.setMandatoryExpMonthsEduWise(draft.getMandatoryExpMonthsEduWise());
        live.setPreferredExpMonthsEduWise(draft.getPreferredExpMonthsEduWise());
        live.setIsProficientInLocalLanguage(draft.getIsProficientInLocalLanguage());
        live.setIsAgeRelWdsWomen(draft.getIsAgeRelWdsWomen());
        live.setIsAgeRelRiotVictimFamily(draft.getIsAgeRelRiotVictimFamily());
        live.setPositionStatus(PositionStatus.ACTIVE);
        live.setDynamicFields(draft.getDynamicFields());
        live.setIsIntermediateRequired(draft.getIsIntermediateRequired());

        Map<String, PositionStateDistributionEntity> existingStatesByKey = new HashMap<>();
        if (live.getPositionStateDistributions() != null) {
            for (PositionStateDistributionEntity existing : live.getPositionStateDistributions()) {
                existingStatesByKey.put(stateKey(existing.getStateId(), existing.getCityId()), existing);
            }
        }
        Set<String> incomingStateKeys = draft.getPositionStateDistributionEditRequests() == null
                ? Collections.emptySet()
                : draft.getPositionStateDistributionEditRequests().stream()
                .map(s -> stateKey(s.getStateId(), s.getCityId()))
                .collect(Collectors.toSet());
        if (live.getPositionStateDistributions() != null) {
            Iterator<PositionStateDistributionEntity> iterator = live.getPositionStateDistributions().iterator();
            while (iterator.hasNext()) {
                PositionStateDistributionEntity existing = iterator.next();
                String key = stateKey(existing.getStateId(), existing.getCityId());
                if (!incomingStateKeys.contains(key)) {
                    existing.clearPositionCategoryDistributions();
                    existing.setJobPosition(null);
                    iterator.remove();
                }
            }
        }
        if (draft.getPositionStateDistributionEditRequests() != null) {
            for (PositionStateDistributionEditRequestEntity s : draft.getPositionStateDistributionEditRequests()) {
                String stateKey = stateKey(s.getStateId(), s.getCityId());
                PositionStateDistributionEntity state = existingStatesByKey.get(stateKey);
                if (state == null || live.getPositionStateDistributions() == null || !live.getPositionStateDistributions().contains(state)) {
                    state = PositionStateDistributionEntity.builder()
                            .jobPosition(live)
                            .stateId(s.getStateId())
                            .cityId(s.getCityId())
                            .totalVacancies(s.getTotalVacancies())
                            .localLanguage(s.getLocalLanguage())
                            .positionCategoryDistributions(new ArrayList<>())
                            .build();
                    live.getPositionStateDistributions().add(state);
                } else {
                    state.setStateId(s.getStateId());
                    state.setCityId(s.getCityId());
                    state.setTotalVacancies(s.getTotalVacancies());
                    state.setLocalLanguage(s.getLocalLanguage());
                    state.clearPositionCategoryDistributions();
                    if (state.getPositionCategoryDistributions() == null) {
                        state.setPositionCategoryDistributions(new ArrayList<>());
                    }
                }

                if (s.getPositionCategoryDistributionEditRequests() != null) {
                    List<PositionCategoryDistributionEntity> cats = new ArrayList<>();
                    for (PositionCategoryDistributionEditRequestEntity c : s.getPositionCategoryDistributionEditRequests()) {
                        PositionCategoryDistributionEntity categoryEntity = PositionCategoryDistributionEntity.builder()
                                .positionStateDistribution(state)
                                .reservationCategoryId(c.getReservationCategoryId())
                                .disabilityCategoryId(c.getDisabilityCategoryId())
                                .vacancyCount(c.getVacancyCount())
                                .isDisability(c.getIsDisability())
                                .build();
                        cats.add(categoryEntity);
                    }
                    state.getPositionCategoryDistributions().addAll(cats);
                }
            }
        }

        Map<String, PositionCategoryNationalDistributionEntity> existingNationalByKey = new HashMap<>();
        if (live.getPositionCategoryNationalDistributions() != null) {
            for (PositionCategoryNationalDistributionEntity existing : live.getPositionCategoryNationalDistributions()) {
                existingNationalByKey.put(categoryKey(existing.getReservationCategoryId(), existing.getDisabilityCategoryId()), existing);
            }
        }
        Set<String> incomingNationalKeys = draft.getPositionCategoryNationalDistributionEditRequests() == null
                ? Collections.emptySet()
                : draft.getPositionCategoryNationalDistributionEditRequests().stream()
                .map(c -> categoryKey(c.getReservationCategoryId(), c.getDisabilityCategoryId()))
                .collect(Collectors.toSet());
        if (live.getPositionCategoryNationalDistributions() != null) {
            Iterator<PositionCategoryNationalDistributionEntity> iterator = live.getPositionCategoryNationalDistributions().iterator();
            while (iterator.hasNext()) {
                PositionCategoryNationalDistributionEntity existing = iterator.next();
                String key = categoryKey(existing.getReservationCategoryId(), existing.getDisabilityCategoryId());
                if (!incomingNationalKeys.contains(key)) {
                    existing.setJobPosition(null);
                    iterator.remove();
                }
            }
        }
        if (draft.getPositionCategoryNationalDistributionEditRequests() != null) {
            for (PositionCategoryNationalDistributionEditRequestEntity c : draft.getPositionCategoryNationalDistributionEditRequests()) {
                String key = categoryKey(c.getReservationCategoryId(), c.getDisabilityCategoryId());
                PositionCategoryNationalDistributionEntity nat = existingNationalByKey.get(key);
                if (nat == null || live.getPositionCategoryNationalDistributions() == null || !live.getPositionCategoryNationalDistributions().contains(nat)) {
                    nat = PositionCategoryNationalDistributionEntity.builder()
                            .jobPosition(live)
                            .reservationCategoryId(c.getReservationCategoryId())
                            .disabilityCategoryId(c.getDisabilityCategoryId())
                            .vacancyCount(c.getVacancyCount())
                            .isDisability(c.getIsDisability())
                            .build();
                    live.getPositionCategoryNationalDistributions().add(nat);
                } else {
                    nat.setReservationCategoryId(c.getReservationCategoryId());
                    nat.setDisabilityCategoryId(c.getDisabilityCategoryId());
                    nat.setVacancyCount(c.getVacancyCount());
                    nat.setIsDisability(c.getIsDisability());
                }
            }
        }
    }

    private void validateImmutablePositionFields(JobPositionEditRequestEntity existing, UpdatePositionEditDraftRequestModel request) {
        if (request.getMasterPositionId() != null && !Objects.equals(request.getMasterPositionId(), existing.getMasterPositionId())) {
            throw new CommonException("Master Position cannot be changed for edited requisition position.");
        }
        if (request.getDeptId() != null && !Objects.equals(request.getDeptId(), existing.getDeptId())) {
            throw new CommonException("Department cannot be changed for edited requisition position.");
        }
        if (request.getIsLocationWise() != null && !Objects.equals(request.getIsLocationWise(), existing.getIsLocationWise())) {
            throw new CommonException("Location distribution type (isLocationWise) cannot be changed.");
        }
    }

    private void applyPositionEditableFields(JobPositionEditRequestEntity existing, UpdatePositionEditDraftRequestModel request) {
        if (request.getTotalVacancies() != null) existing.setTotalVacancies(request.getTotalVacancies());
        if (request.getEligibilityAgeMin() != null) existing.setEligibilityAgeMin(request.getEligibilityAgeMin());
        if (request.getEligibilityAgeMax() != null) existing.setEligibilityAgeMax(request.getEligibilityAgeMax());
        if (request.getEmploymentType() != null) existing.setEmploymentType(request.getEmploymentType());
        if (request.getCibilScore() != null) existing.setCibilScore(request.getCibilScore());
        if (request.getGradeId() != null) existing.setGradeId(request.getGradeId());
        if (request.getMandatoryEducation() != null) existing.setMandatoryEducation(request.getMandatoryEducation());
        if (request.getPreferredEducation() != null) existing.setPreferredEducation(request.getPreferredEducation());
        if (request.getMandatoryExperience() != null) existing.setMandatoryExperience(request.getMandatoryExperience());
        if (request.getPreferredExperience() != null) existing.setPreferredExperience(request.getPreferredExperience());
        if (request.getRolesResponsibilities() != null) existing.setRolesResponsibilities(request.getRolesResponsibilities());
        if (request.getContractYears() != null) existing.setContractYears(request.getContractYears());
        if (request.getIsLocationPreferenceEnabled() != null) existing.setIsLocationPreferenceEnabled(request.getIsLocationPreferenceEnabled());
        if (request.getIsMandatoryExpMonthsEduWise() != null) existing.setIsMandatoryExpMonthsEduWise(request.getIsMandatoryExpMonthsEduWise());
        if (request.getIsPreferredExpMonthsEduWise() != null) existing.setIsPreferredExpMonthsEduWise(request.getIsPreferredExpMonthsEduWise());
        if (request.getTotalExperience() != null) existing.setTotalExperience(request.getTotalExperience());
        if (request.getMandatoryExperienceMonths() != null) existing.setMandatoryExperienceMonths(request.getMandatoryExperienceMonths());
        if (request.getPreferredExperienceMonths() != null) existing.setPreferredExperienceMonths(request.getPreferredExperienceMonths());
        if (request.getIndentPath() != null) existing.setIndentPath(request.getIndentPath());
        if (request.getApprovedBy() != null) existing.setApprovedBy(request.getApprovedBy());
        if (request.getApprovedOn() != null) existing.setApprovedOn(request.getApprovedOn());
        if (request.getIsMedicalRequired() != null) existing.setIsMedicalRequired(request.getIsMedicalRequired());
        if (request.getMandatoryEduRulesJson() != null) existing.setMandatoryEduRulesJson(request.getMandatoryEduRulesJson());
        if (request.getPreferredEduRulesJson() != null) existing.setPreferredEduRulesJson(request.getPreferredEduRulesJson());
        if (request.getMandatoryExpMonthsEduWise() != null) existing.setMandatoryExpMonthsEduWise(request.getMandatoryExpMonthsEduWise());
        if (request.getPreferredExpMonthsEduWise() != null) existing.setPreferredExpMonthsEduWise(request.getPreferredExpMonthsEduWise());
        if (request.getIndentName() != null) existing.setIndentName(request.getIndentName());
        if (request.getIndentOthers() != null) existing.setIndentOthers(request.getIndentOthers());
        if (request.getIsProficientInLocalLanguage() != null) existing.setIsProficientInLocalLanguage(request.getIsProficientInLocalLanguage());
        if (request.getIsAgeRelWdsWomen() != null) existing.setIsAgeRelWdsWomen(request.getIsAgeRelWdsWomen());
        if (request.getIsAgeRelRiotVictimFamily() != null) existing.setIsAgeRelRiotVictimFamily(request.getIsAgeRelRiotVictimFamily());
        if (request.getIsIntermediateRequired() != null) existing.setIsIntermediateRequired(request.getIsIntermediateRequired());
        if (request.getDynamicFields() != null) existing.setDynamicFields(request.getDynamicFields());

    }
    private void applyExclusionsDraftFields(JobPositionEditRequestEntity draftPosition, UpdatePositionEditDraftRequestModel request) {

        List<JobPositionExclusionsEditRequestEntity> exclusions =
                draftPosition.getJobPositionExclusionsEditRequestEntity();

        exclusions.clear();

        if (request.getExclusions() != null) {
            exclusions.addAll(
                    request.getExclusions().stream()
                            .map(exclusion -> JobPositionExclusionsEditRequestEntity.builder()
                                    .jobEditPosition(draftPosition)
                                    .exclusionId(exclusion.getExclusionId())
                                    .isExcluded(exclusion.getIsExcluded())
                                    .build())
                            .toList()
            );
        }
    }

    private void applyDistributionDraftFields(JobPositionEditRequestEntity draftPosition, UUID parentPositionId, UpdatePositionEditDraftRequestModel request) {
        JobPositionsEntity livePosition = positionsRepository.findById(parentPositionId)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found."));

        if (request.getStateDistributions() != null) {
            validateStateDistributionRules(livePosition, request.getStateDistributions());
            Map<String, PositionStateDistributionEditRequestEntity> existingStatesByKey = new HashMap<>();
            for (PositionStateDistributionEditRequestEntity existingState : draftPosition.getPositionStateDistributionEditRequests()) {
                existingStatesByKey.put(stateKey(existingState.getStateId(), existingState.getCityId()), existingState);
            }
            Set<String> incomingStateKeys = request.getStateDistributions().stream()
                    .map(s -> stateKey(s.getStateId(), s.getCityId()))
                    .collect(Collectors.toSet());

            Iterator<PositionStateDistributionEditRequestEntity> stateIterator = draftPosition.getPositionStateDistributionEditRequests().iterator();
            while (stateIterator.hasNext()) {
                PositionStateDistributionEditRequestEntity existingState = stateIterator.next();
                String key = stateKey(existingState.getStateId(), existingState.getCityId());
                if (!incomingStateKeys.contains(key)) {
                    if (existingState.getPositionCategoryDistributionEditRequests() != null) {
                        for (PositionCategoryDistributionEditRequestEntity existingCat : existingState.getPositionCategoryDistributionEditRequests()) {
                            existingCat.setStateDistribution(null);
                            existingCat.setJobEditPosition(null);
                        }
                        existingState.getPositionCategoryDistributionEditRequests().clear();
                    }
                    existingState.setJobEditPosition(null);
                    stateIterator.remove();
                }
            }

            for (PositionStateDistributionEditDraftRequestModel stateModel : request.getStateDistributions()) {
                String stateKey = stateKey(stateModel.getStateId(), stateModel.getCityId());
                PositionStateDistributionEditRequestEntity state = existingStatesByKey.get(stateKey);
                if (state == null || !draftPosition.getPositionStateDistributionEditRequests().contains(state)) {
                    state = PositionStateDistributionEditRequestEntity.builder()
                            .jobEditPosition(draftPosition)
                            .stateId(stateModel.getStateId())
                            .cityId(stateModel.getCityId())
                            .totalVacancies(stateModel.getTotalVacancies())
                            .localLanguage(stateModel.getLocalLanguage())
                            .build();
                    draftPosition.getPositionStateDistributionEditRequests().add(state);
                } else {
                    state.setJobEditPosition(draftPosition);
                    state.setStateId(stateModel.getStateId());
                    state.setCityId(stateModel.getCityId());
                    state.setTotalVacancies(stateModel.getTotalVacancies());
                    state.setLocalLanguage(stateModel.getLocalLanguage());
                }

                if (stateModel.getCategories() != null) {
                    Map<String, PositionCategoryDistributionEditRequestEntity> existingCategoriesByKey = new HashMap<>();
                    if (state.getPositionCategoryDistributionEditRequests() != null) {
                        for (PositionCategoryDistributionEditRequestEntity existingCat : state.getPositionCategoryDistributionEditRequests()) {
                            existingCategoriesByKey.put(categoryKey(existingCat.getReservationCategoryId(), existingCat.getDisabilityCategoryId()), existingCat);
                        }
                    }
                    Set<String> incomingCategoryKeys = stateModel.getCategories().stream()
                            .map(cat -> categoryKey(cat.getReservationCategoryId(), cat.getDisabilityCategoryId()))
                            .collect(Collectors.toSet());

                    if (state.getPositionCategoryDistributionEditRequests() != null) {
                        Iterator<PositionCategoryDistributionEditRequestEntity> categoryIterator = state.getPositionCategoryDistributionEditRequests().iterator();
                        while (categoryIterator.hasNext()) {
                            PositionCategoryDistributionEditRequestEntity existingCat = categoryIterator.next();
                            String key = categoryKey(existingCat.getReservationCategoryId(), existingCat.getDisabilityCategoryId());
                            if (!incomingCategoryKeys.contains(key)) {
                                existingCat.setStateDistribution(null);
                                existingCat.setJobEditPosition(null);
                                categoryIterator.remove();
                            }
                        }
                    }

                    for (PositionCategoryEditDraftRequestModel catModel : stateModel.getCategories()) {
                        String key = categoryKey(catModel.getReservationCategoryId(), catModel.getDisabilityCategoryId());
                        PositionCategoryDistributionEditRequestEntity cat = existingCategoriesByKey.get(key);
                        if (cat == null || state.getPositionCategoryDistributionEditRequests() == null || !state.getPositionCategoryDistributionEditRequests().contains(cat)) {
                            cat = PositionCategoryDistributionEditRequestEntity.builder()
                                    .jobEditPosition(draftPosition)
                                    .stateDistribution(state)
                                    .reservationCategoryId(catModel.getReservationCategoryId())
                                    .disabilityCategoryId(catModel.getDisabilityCategoryId())
                                    .vacancyCount(catModel.getVacancyCount())
                                    .isDisability(Boolean.TRUE.equals(catModel.getIsDisability()))
                                    .build();
                            state.getPositionCategoryDistributionEditRequests().add(cat);
                        } else {
                            cat.setJobEditPosition(draftPosition);
                            cat.setStateDistribution(state);
                            cat.setReservationCategoryId(catModel.getReservationCategoryId());
                            cat.setDisabilityCategoryId(catModel.getDisabilityCategoryId());
                            cat.setVacancyCount(catModel.getVacancyCount());
                            cat.setIsDisability(Boolean.TRUE.equals(catModel.getIsDisability()));
                        }
                    }
                }
            }
        }

        if (request.getNationalDistributions() != null) {
            validateNationalDistributionRules(livePosition, request.getNationalDistributions());
            Map<String, PositionCategoryNationalDistributionEditRequestEntity> existingNationalByKey = new HashMap<>();
            for (PositionCategoryNationalDistributionEditRequestEntity existingCat : draftPosition.getPositionCategoryNationalDistributionEditRequests()) {
                existingNationalByKey.put(categoryKey(existingCat.getReservationCategoryId(), existingCat.getDisabilityCategoryId()), existingCat);
            }
            Set<String> incomingNationalKeys = request.getNationalDistributions().stream()
                    .map(cat -> categoryKey(cat.getReservationCategoryId(), cat.getDisabilityCategoryId()))
                    .collect(Collectors.toSet());

            Iterator<PositionCategoryNationalDistributionEditRequestEntity> nationalIterator = draftPosition.getPositionCategoryNationalDistributionEditRequests().iterator();
            while (nationalIterator.hasNext()) {
                PositionCategoryNationalDistributionEditRequestEntity existingCat = nationalIterator.next();
                String key = categoryKey(existingCat.getReservationCategoryId(), existingCat.getDisabilityCategoryId());
                if (!incomingNationalKeys.contains(key)) {
                    existingCat.setJobEditPosition(null);
                    nationalIterator.remove();
                }
            }

            for (PositionCategoryEditDraftRequestModel catModel : request.getNationalDistributions()) {
                String key = categoryKey(catModel.getReservationCategoryId(), catModel.getDisabilityCategoryId());
                PositionCategoryNationalDistributionEditRequestEntity cat = existingNationalByKey.get(key);
                if (cat == null || !draftPosition.getPositionCategoryNationalDistributionEditRequests().contains(cat)) {
                    cat = PositionCategoryNationalDistributionEditRequestEntity.builder()
                            .jobEditPosition(draftPosition)
                            .reservationCategoryId(catModel.getReservationCategoryId())
                            .disabilityCategoryId(catModel.getDisabilityCategoryId())
                            .vacancyCount(catModel.getVacancyCount())
                            .isDisability(Boolean.TRUE.equals(catModel.getIsDisability()))
                            .build();
                    draftPosition.getPositionCategoryNationalDistributionEditRequests().add(cat);
                } else {
                    cat.setJobEditPosition(draftPosition);
                    cat.setReservationCategoryId(catModel.getReservationCategoryId());
                    cat.setDisabilityCategoryId(catModel.getDisabilityCategoryId());
                    cat.setVacancyCount(catModel.getVacancyCount());
                    cat.setIsDisability(Boolean.TRUE.equals(catModel.getIsDisability()));
                }
            }
        }
    }

    private void validateStateDistributionRules(JobPositionsEntity livePosition, List<PositionStateDistributionEditDraftRequestModel> incomingStates) {
        Map<String, PositionStateDistributionEntity> liveStateMap = new HashMap<>();
        if (livePosition.getPositionStateDistributions() != null) {
            for (PositionStateDistributionEntity state : livePosition.getPositionStateDistributions()) {
                liveStateMap.put(stateKey(state.getStateId(), state.getCityId()), state);
            }
        }
        Set<String> incomingKeys = incomingStates.stream()
                .map(s -> stateKey(s.getStateId(), s.getCityId()))
                .collect(Collectors.toSet());

        for (String liveKey : liveStateMap.keySet()) {
            if (!incomingKeys.contains(liveKey)) {
                throw new CommonException("Existing state distribution cannot be removed.");
            }
        }

        for (PositionStateDistributionEditDraftRequestModel incoming : incomingStates) {
            PositionStateDistributionEntity liveState = liveStateMap.get(stateKey(incoming.getStateId(), incoming.getCityId()));
            if (liveState == null || liveState.getPositionCategoryDistributions() == null) {
                continue;
            }
            Map<String, Integer> incomingCategoryCount = new HashMap<>();
            if (incoming.getCategories() != null) {
                for (PositionCategoryEditDraftRequestModel cat : incoming.getCategories()) {
                    incomingCategoryCount.put(categoryKey(cat.getReservationCategoryId(), cat.getDisabilityCategoryId()), cat.getVacancyCount());
                }
            }
            for (PositionCategoryDistributionEntity liveCategory : liveState.getPositionCategoryDistributions()) {
                if (liveCategory.getVacancyCount() != null && liveCategory.getVacancyCount() > 0) {
                    String key = categoryKey(liveCategory.getReservationCategoryId(), liveCategory.getDisabilityCategoryId());
                    Integer incomingCount = incomingCategoryCount.get(key);
                    if (incomingCount != null && incomingCount == 0) {
                        throw new CommonException("Existing non-zero category vacancy cannot be reduced to zero.");
                    }
                }
            }
        }
    }

    private void validateNationalDistributionRules(JobPositionsEntity livePosition, List<PositionCategoryEditDraftRequestModel> incomingNational) {
        Map<String, Integer> incomingMap = new HashMap<>();
        for (PositionCategoryEditDraftRequestModel cat : incomingNational) {
            incomingMap.put(categoryKey(cat.getReservationCategoryId(), cat.getDisabilityCategoryId()), cat.getVacancyCount());
        }

        if (livePosition.getPositionCategoryNationalDistributions() == null) {
            return;
        }
        for (PositionCategoryNationalDistributionEntity liveCategory : livePosition.getPositionCategoryNationalDistributions()) {
            if (liveCategory.getVacancyCount() != null && liveCategory.getVacancyCount() > 0) {
                String key = categoryKey(liveCategory.getReservationCategoryId(), liveCategory.getDisabilityCategoryId());
                Integer incomingCount = incomingMap.get(key);
                if (incomingCount != null && incomingCount == 0) {
                    throw new CommonException("Existing non-zero national category vacancy cannot be reduced to zero.");
                }
            }
        }
    }

    private String stateKey(UUID stateId, UUID cityId) {
        return String.valueOf(stateId) + "::" + String.valueOf(cityId);
    }

    private String categoryKey(UUID reservationCategoryId, UUID disabilityCategoryId) {
        return String.valueOf(reservationCategoryId) + "::" + String.valueOf(disabilityCategoryId);
    }

    private WorkflowApprovalEntity createWorkflowEntity(JobRequisitionEditRequestEntity draft) {
        return WorkflowApprovalEntity.builder()
                .entityId(draft.getId())
                .entityType(JobRequisitionEditRequestEntity.ENTITY_TYPE)
                .stepNumber(1)
                .approverRole(securityUtils.getCurrentUserRole())
                .approverId(securityUtils.getCurrentUserId())
                .action(DBConstants.WORKFLOW_ACTION_UPDATE)
                .actionDate(LocalDateTime.now())
                .comments(draft.getRequisitionComments())
                .status(draft.getRequisitionStatus().name())
                .build();
    }

    private RequisitionEditDraftResponseModel toResponse(JobRequisitionEditRequestEntity draft) {
        List<RequisitionEditPositionResponseModel> positions = draft.getPositionEditRequests() == null
                ? Collections.emptyList()
                : draft.getPositionEditRequests().stream().map(pos -> RequisitionEditPositionResponseModel.builder()
                .positionEditRequestId(pos.getId())
                .parentPositionId(pos.getParentPositionId())
                .masterPositionId(pos.getMasterPositionId())
                .deptId(pos.getDeptId())
                .totalVacancies(pos.getTotalVacancies())
                .isLocationWise(pos.getIsLocationWise())
                .build()).toList();

        return RequisitionEditDraftResponseModel.builder()
                .draftId(draft.getId())
                .parentRequisitionId(draft.getParentRequisitionId())
                .baseVersionNo(draft.getBaseVersionNo())
                .requisitionStatus(draft.getRequisitionStatus())
                .requisitionTitle(draft.getRequisitionTitle())
                .requisitionDescription(draft.getRequisitionDescription())
                .startDate(draft.getStartDate())
                .endDate(draft.getEndDate())
                .requisitionComments(draft.getRequisitionComments())
                .indentPath(draft.getIndentPath())
                .cutoffDate(draft.getCutoffDate())
                .submittedAt(draft.getSubmittedAt())
                .approvedAt(draft.getApprovedAt())
                .publishedAt(draft.getPublishedAt())
                .publishedBy(draft.getPublishedBy())
                .positions(positions)
                .build();
    }
}
