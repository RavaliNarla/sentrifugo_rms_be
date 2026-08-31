package com.bob.jobportal.service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.EmbeddingBuilderService;
import com.bob.commonutil.service.MailSenderHelper;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.JobRequisitionsDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.*;
import com.bob.db.mapper.JobRequisitionMapper;
import com.bob.db.repository.*;
import com.bob.db.util.DBConstants;
import com.bob.jobportal.model.JobPostingRequestModel;
import jakarta.mail.MessagingException;
import com.bob.jobportal.model.JobRequisitionWithDraftResponseModel;
import com.bob.jobportal.model.RequisitionEditDraftResponseModel;
import com.bob.jobportal.model.RequisitionEditPositionResponseModel;
import com.bob.jobportal.model.RequisitionReinitializeRequestModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;


import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JobRequisitionsService {
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
    private PositionsRepository positionsRepository;

    @Autowired
    private PositionCategoryDistributionRepository positionCategoryDistributionRepository;

    @Autowired
    private JobRequisitionEditRequestRepository jobRequisitionEditRequestRepository;

    @Autowired
    private JobRequisitionMapper jobRequisitionMapper;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RequisitionApproversRepository requisitionApproversRepository;

    @Autowired
    private WorkflowApprovalEntityRepository workflowApprovalEntityRepository;

    @Autowired
    private PositionPanelRepository positionPanelRepository;

    @Autowired
    private InterviewPanelMembersRepository interviewPanelMembersRepository;

    @Autowired
    private EmbeddingBuilderService embeddingBuilderService;

    @Autowired
    private MailSenderHelper mailSenderHelper;

    @Transactional
    public void createJobRequisition(JobRequisitionsDTO requisitionDTO){
        
        // Validate required fields
        validateRequisitionDTO(requisitionDTO);

        JobRequisitionsEntity requisition = jobRequisitionMapper.toEntity(requisitionDTO);
        requisition.setRequisitionStatus(RequisitionStatus.NEW);

        jobRequisitionsRepository.save(requisition);
        log.info("Job requisition created successfully with title: {}", requisitionDTO.getRequisitionTitle());
    }

    /**
     * Creates a new live requisition linked to a ended parent, copying selected job positions (including
     * state / category / national distribution rows) into the new requisition.
     */
    @Transactional
    public JobRequisitionsDTO reinitializeRequisition(RequisitionReinitializeRequestModel request) {
        List<UUID> uniqueOrdered = new ArrayList<>(new LinkedHashSet<>(request.getPositionIds()));
        if (uniqueOrdered.isEmpty()) {
            throw new CommonException("At least one position must be selected.");
        }
        if (uniqueOrdered.size() != request.getPositionIds().size()) {
            throw new CommonException("Duplicate position ids are not allowed.");
        }

        JobRequisitionsDTO shape = new JobRequisitionsDTO();
        shape.setRequisitionTitle(request.getRequisitionTitle());
        shape.setRequisitionDescription(request.getRequisitionDescription());
        shape.setStartDate(request.getStartDate());
        shape.setEndDate(request.getEndDate());
        validateRequisitionDTO(shape);

        UUID parentId = request.getParentRequisitionId();
        JobRequisitionsEntity parent = jobRequisitionsRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent job requisition not found"));

        if (parent.getRequisitionStatus() != RequisitionStatus.CLOSED) {
            throw new CommonException("Only CLOSED requisitions can be reinitialized. Use the 'Close requisition' to close the requisition first.");
        }
        if (Boolean.TRUE.equals(parent.getIsReinitialized())) {
            throw new CommonException("This requisition has already been reinitialized and cannot be reinitialized again.");
        }
        LocalDate today = LocalDate.now();
        if (parent.getEndDate() == null) {
            throw new CommonException("Parent requisition has no end date and cannot be reinitialized.");
        }
        if (!parent.getEndDate().isBefore(today)) {
            throw new CommonException("Parent requisition must have ended before it can be reinitialized.");
        }

        JobRequisitionsEntity child = JobRequisitionsEntity.builder()
                .requisitionTitle(request.getRequisitionTitle().trim())
                .requisitionDescription(request.getRequisitionDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .cutoffDate(request.getCutoffDate())
                .requisitionStatus(RequisitionStatus.NEW)
                .requisitionComments(null)
                .isInEditMode(false)
                .parentRequisitionId(parent.getId())
                .isReinitialized(false)
                .build();

        child = jobRequisitionsRepository.save(child);

        // Mark the parent as reinitialized so it cannot be reinitialized again.
        parent.setIsReinitialized(true);
        jobRequisitionsRepository.save(parent);

        List<JobPositionsEntity> withStates = positionsRepository.findAllByIdInWithStateDistributionsOnly(uniqueOrdered);
        List<UUID> stateIds = withStates.stream()
                .filter(jp -> jp.getPositionStateDistributions() != null)
                .flatMap(jp -> jp.getPositionStateDistributions().stream())
                .map(PositionStateDistributionEntity::getId)
                .toList();
        if (!stateIds.isEmpty()) {
            positionCategoryDistributionRepository.findAllByStateDistributionIdInWithFetch(stateIds);
        }
        positionsRepository.findAllByIdInWithNationalDistributions(uniqueOrdered);

        if (withStates.size() != uniqueOrdered.size()) {
            Set<UUID> found = withStates.stream().map(JobPositionsEntity::getId).collect(Collectors.toSet());
            List<UUID> missing = uniqueOrdered.stream().filter(id -> !found.contains(id)).toList();
            throw new CommonException("Job positions not found or inactive: " + missing);
        }
        Map<UUID, JobPositionsEntity> byId = withStates.stream()
                .collect(Collectors.toMap(JobPositionsEntity::getId, p -> p));

        for (UUID positionId : uniqueOrdered) {
            JobPositionsEntity source = byId.get(positionId);
            if (!parentId.equals(source.getRequisitionId())) {
                throw new CommonException("Selected position does not belong to the parent requisition.");
            }
            // Reject positions where all vacancies are already filled (remaining = 0).
            int effectiveRemaining = effectiveVacancy(source.getRemainingTotalVacancies(), source.getTotalVacancies());
            if (effectiveRemaining <= 0) {
                throw new CommonException("Position " + positionId + " has no remaining vacancies — all seats are filled and it cannot be reinitialized.");
            }
            positionsRepository.save(buildReinitializedJobPositionClone(source, child.getId()));
        }

        log.info("Requisition reinitialized from parent {} into new requisition {}", parentId, child.getId());
        return getRequisitionById(child.getId());
    }

    /**
     * Returns the effective vacancy count for reinitialize cloning:
     * use remaining_* if it has been initialized (not null), otherwise fall back to the original total.
     */
    private static int effectiveVacancy(Integer remaining, Integer total) {
        return (remaining != null) ? remaining : (total != null ? total : 0);
    }

    private static JobPositionsEntity buildReinitializedJobPositionClone(JobPositionsEntity source, UUID newRequisitionId) {
        List<PositionStateDistributionEntity> newStates = new ArrayList<>();
        if (source.getPositionStateDistributions() != null) {
            for (PositionStateDistributionEntity s : source.getPositionStateDistributions()) {
                List<PositionCategoryDistributionEntity> cats = new ArrayList<>();
                PositionStateDistributionEntity state = PositionStateDistributionEntity.builder()
                        .stateId(s.getStateId())
                        .cityId(s.getCityId())
                        .totalVacancies(effectiveVacancy(s.getRemainingTotalVacancies(), s.getTotalVacancies()))
                        .remainingTotalVacancies(null)
                        .localLanguage(s.getLocalLanguage())
                        .positionCategoryDistributions(cats)
                        .build();
                if (s.getPositionCategoryDistributions() != null) {
                    for (PositionCategoryDistributionEntity c : s.getPositionCategoryDistributions()) {
                        cats.add(PositionCategoryDistributionEntity.builder()
                                .positionStateDistribution(state)
                                .reservationCategoryId(c.getReservationCategoryId())
                                .disabilityCategoryId(c.getDisabilityCategoryId())
                                .vacancyCount(effectiveVacancy(c.getRemainingVacancyCount(), c.getVacancyCount()))
                                .remainingVacancyCount(null)
                                .isDisability(c.getIsDisability())
                                .build());
                    }
                }
                newStates.add(state);
            }
        }

        List<PositionCategoryNationalDistributionEntity> newNational = new ArrayList<>();
        if (source.getPositionCategoryNationalDistributions() != null) {
            for (PositionCategoryNationalDistributionEntity n : source.getPositionCategoryNationalDistributions()) {
                newNational.add(PositionCategoryNationalDistributionEntity.builder()
                        .reservationCategoryId(n.getReservationCategoryId())
                        .disabilityCategoryId(n.getDisabilityCategoryId())
                        .vacancyCount(effectiveVacancy(n.getRemainingVacancyCount(), n.getVacancyCount()))
                        .remainingVacancyCount(null)
                        .isDisability(n.getIsDisability())
                        .build());
            }
        }

        JobPositionsEntity target = JobPositionsEntity.builder()
                .requisitionId(newRequisitionId)
                .parentPositionId(source.getId())
                .masterPositionId(source.getMasterPositionId())
                .deptId(source.getDeptId())
                .totalVacancies(effectiveVacancy(source.getRemainingTotalVacancies(), source.getTotalVacancies()))
                .remainingTotalVacancies(null)
                .isHiringCompleted(false)
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
                .positionStatus(PositionStatus.NEW)
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
                .positionStateDistributions(newStates)
                .positionCategoryNationalDistributions(newNational)
                .build();

        for (PositionStateDistributionEntity st : newStates) {
            st.setJobPosition(target);
        }
        for (PositionCategoryNationalDistributionEntity nat : newNational) {
            nat.setJobPosition(target);
        }
        return target;
    }

    private void validateRequisitionDTO(JobRequisitionsDTO dto) {
        if (dto.getRequisitionTitle() == null || dto.getRequisitionTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Requisition title is required");
        }
        if (dto.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (dto.getEndDate() == null) {
            throw new IllegalArgumentException("End date is required");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }

    public Page<JobRequisitionsDTO> searchRequisitions(Integer year,Integer month, List<RequisitionStatus> statuses, String searchTerm,UUID departmentId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<JobRequisitionsEntity> requisitionsPage = jobRequisitionsRepository.findRequisitionsWithFilters(year, month,statuses, searchTerm,departmentId, pageable);
        
        // Get all requisition IDs for fetching positions
        List<UUID> requisitionIds = requisitionsPage.getContent().stream()
                .map(JobRequisitionsEntity::getId)
                .collect(Collectors.toList());
        
        // Fetch all positions for these requisitions in one query
        List<JobPositionsEntity> allPositions = positionsRepository.findByRequisitionIdIn(requisitionIds);
        
        // Group positions by requisition ID
        Map<UUID, List<JobPositionsEntity>> positionsByRequisition = allPositions.stream()
                .collect(Collectors.groupingBy(JobPositionsEntity::getRequisitionId));
        
        // Convert to DTOs and populate aggregated counts
        List<JobRequisitionsDTO> responseDTOs = requisitionsPage.getContent().stream()
                .map(requisition -> {
                    JobRequisitionsDTO dto = jobRequisitionMapper.toDTO(requisition);
                    List<JobPositionsEntity> positions = positionsByRequisition.get(requisition.getId());

                    calculateCounts(dto,positions); //TODO: move this to Repo layer

                    return dto;
                })
                .collect(Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(responseDTOs, pageable, requisitionsPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<JobRequisitionWithDraftResponseModel> searchRequisitionsWithDrafts(Integer year, Integer month, List<RequisitionStatus> statuses, String searchTerm, UUID departmentId, int page, int size) {
        Page<JobRequisitionsDTO> basePage = searchRequisitions(year, month, statuses, searchTerm, departmentId, page, size);

        List<JobRequisitionWithDraftResponseModel> response = basePage.getContent().stream()
                .map(this::toRequisitionWithDraftResponse)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(response, basePage.getPageable(), basePage.getTotalElements());
    }

    public JobRequisitionsDTO getRequisitionById(UUID id) {
        JobRequisitionsEntity requisition = jobRequisitionsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job requisition not found"));
        
        JobRequisitionsDTO dto = jobRequisitionMapper.toDTO(requisition);
        
        // Calculate aggregated counts
        List<JobPositionsEntity> positions = positionsRepository.findByRequisitionIdIn(List.of(id));

        calculateCounts(dto,positions); //TODO: move this to Repo layer
        
        return dto;
    }

    public void calculateCounts(JobRequisitionsDTO dto,  List<JobPositionsEntity> positions){  //TODO: move this to Repo layer
        if (positions != null && !positions.isEmpty()) {
            dto.setDepartmentCount((int) positions.stream()
                    .map(JobPositionsEntity::getDeptId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count());
            dto.setHasDraftPositions(positions.stream().filter((pos)->pos.getPositionStatus() == PositionStatus.DRAFT).findAny().isPresent());

            dto.setPositionCount(positions.size());

            dto.setVacancyCount(positions.stream()
                    .map(JobPositionsEntity::getTotalVacancies)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum());
        } else {
            dto.setDepartmentCount(0);
            dto.setPositionCount(0);
            dto.setVacancyCount(0);
        }
    }

    @Transactional
    public void updateJobRequisition(UUID id, JobRequisitionsDTO requisitionDTO){
        // Find existing requisition
        JobRequisitionsEntity existingRequisition = jobRequisitionsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job requisition not found with id: " + id));

        // Validate updated data
        validateRequisitionDTO(requisitionDTO);

        // Store old entity for audit
        JobRequisitionsEntity oldRequisitionEntity = existingRequisition.toBuilder().build();

        // Update fields
        jobRequisitionMapper.updateEntityFromDto(requisitionDTO, existingRequisition);

        jobRequisitionsRepository.saveWithAudit(oldRequisitionEntity, existingRequisition);
        log.info("Job requisition updated successfully: {}", id);
    }

    @Transactional
    public void deleteJobRequisition(UUID id) {
        JobRequisitionsEntity requisition = jobRequisitionsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job requisition not found."));

        // If this req is a child (reinitialized from a parent), release the parent's
        // is_reinitialized flag so the parent can be reinitialized again.
        if (requisition.getParentRequisitionId() != null) {
            jobRequisitionsRepository.findById(requisition.getParentRequisitionId()).ifPresent(parent -> {
                if (Boolean.TRUE.equals(parent.getIsReinitialized())) {
                    parent.setIsReinitialized(false);
                    jobRequisitionsRepository.save(parent);
                    log.info("Parent requisition {} is_reinitialized reset to false after child {} was deleted.",
                            parent.getId(), id);
                }
            });
        }

        // Soft delete via @SQLDelete annotation
        jobRequisitionsRepository.delete(requisition);
        log.info("Job requisition soft deleted: {}", id);
    }

    @Transactional
    public void closeRequisition(UUID id) {
        JobRequisitionsEntity requisition = jobRequisitionsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job requisition not found."));

        LocalDate today = LocalDate.now();
        if (requisition.getEndDate() == null || !requisition.getEndDate().isBefore(today)) {
            throw new CommonException("Requisition can only be closed after its end date has passed (end date must be before today).");
        }
        if (requisition.getRequisitionStatus() == RequisitionStatus.CLOSED) {
            throw new CommonException("Requisition is already closed.");
        }

        JobRequisitionsEntity old = requisition.toBuilder().build();
        requisition.setRequisitionStatus(RequisitionStatus.CLOSED);
        jobRequisitionsRepository.saveWithAudit(old, requisition);
        log.info("Requisition {} closed.", id);
    }

    @Transactional
    public List<JobRequisitionWithDraftResponseModel> approveJobRequisitions(JobPostingRequestModel jobPostingRequestModel) throws MessagingException, IOException {

        List<UUID> reqIds = jobPostingRequestModel.getJobRequisitionIds();
        List<JobRequisitionsEntity> allEntities = jobRequisitionsRepository.findAllByIdIn(reqIds);

        if (allEntities.isEmpty()) {
            throw new ResourceNotFoundException("No requisitions found.");
        }

        // Split batch: reqs without an active edit draft go through the normal path;
        // reqs with is_in_edit_mode=true route to the draft approval path.
        List<JobRequisitionsEntity> normalReqs = allEntities.stream()
                .filter(e -> !Boolean.TRUE.equals(e.getIsInEditMode()))
                .collect(Collectors.toList());
        List<JobRequisitionsEntity> editModeReqs = allEntities.stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsInEditMode()))
                .collect(Collectors.toList());

        UUID currentUser = securityUtils.getCurrentUserId();
        RequisitionApproversEntity approversEntity = requisitionApproversRepository.findByApproverId(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Current user is not configured as a requisition approver."));

        RequisitionApproversEntity.ApproverRole role = approversEntity.getApproverRole();
        RequisitionStatus requestedStatus = jobPostingRequestModel.getPostingStatus();

        if (!(role.equals(RequisitionApproversEntity.ApproverRole.L1)
                || role.equals(RequisitionApproversEntity.ApproverRole.L2))) {
            throw new IllegalStateException("User has an unsupported approver role: " + role);
        }

        // Determine target statuses for both paths from the single postingStatus in the request.
        RequisitionStatus normalTargetStatus = null;
        RequisitionStatus normalRequiredCurrentStatus = null;
        RequisitionEditStatus draftTargetStatus;
        RequisitionEditStatus draftRequiredCurrentStatus = null;

        if (role.equals(RequisitionApproversEntity.ApproverRole.L1)) {
            if (requestedStatus.equals(RequisitionStatus.L2_PENDING)) {
                normalTargetStatus = RequisitionStatus.L2_PENDING;
                normalRequiredCurrentStatus = RequisitionStatus.L1_PENDING;
                draftTargetStatus = RequisitionEditStatus.L2_PENDING;
                draftRequiredCurrentStatus = RequisitionEditStatus.L1_PENDING;
            } else if (requestedStatus.equals(RequisitionStatus.L1_REJECTED)) {
                normalTargetStatus = RequisitionStatus.L1_REJECTED;
                draftTargetStatus = RequisitionEditStatus.L1_REJECTED;
            } else {
                throw new IllegalStateException(
                    String.format("L1 approver cannot perform action '%s'. Allowed actions: %s, %s.",
                        requestedStatus, RequisitionStatus.L2_PENDING, RequisitionStatus.L1_REJECTED));
            }
        } else {
            if (requestedStatus.equals(RequisitionStatus.APPROVED)) {
                normalTargetStatus = RequisitionStatus.APPROVED;
                normalRequiredCurrentStatus = RequisitionStatus.L2_PENDING;
                draftTargetStatus = RequisitionEditStatus.APPROVED;
                draftRequiredCurrentStatus = RequisitionEditStatus.L2_PENDING;
            } else if (requestedStatus.equals(RequisitionStatus.L2_REJECTED)) {
                normalTargetStatus = RequisitionStatus.L2_REJECTED;
                draftTargetStatus = RequisitionEditStatus.L2_REJECTED;
            } else {
                throw new IllegalStateException(
                    String.format("L2 approver cannot perform action '%s'. Allowed actions: %s, %s.",
                        requestedStatus, RequisitionStatus.APPROVED, RequisitionStatus.L2_REJECTED));
            }
        }

        List<WorkflowApprovalEntity> workflowEntities = new ArrayList<>();

        // --- Normal requisition path (unchanged logic) ---
        List<JobRequisitionsEntity> savedNormalEntities = new ArrayList<>();
        for (JobRequisitionsEntity entity : normalReqs) {
            if (normalRequiredCurrentStatus != null && entity.getRequisitionStatus() != normalRequiredCurrentStatus) {
                throw new IllegalStateException(
                    String.format("Cannot process requisition. Must be in '%s' state. Found '%s'.",
                    normalRequiredCurrentStatus, entity.getRequisitionStatus()));
            }
            JobRequisitionsEntity oldEntity = entity.toBuilder().build();
            entity.setRequisitionStatus(normalTargetStatus);
            entity.setRequisitionComments(jobPostingRequestModel.getComments());
            workflowEntities.add(createWorkflowEntityCandidateApplication(entity));
            savedNormalEntities.add(jobRequisitionsRepository.saveWithAudit(oldEntity, entity));
        }

        // Activate positions + generate embeddings only for normal reqs on final approval.
        List<UUID> normalApprovedIds = savedNormalEntities.stream()
                .filter(e -> RequisitionStatus.APPROVED.equals(e.getRequisitionStatus()))
                .map(JobRequisitionsEntity::getId)
                .collect(Collectors.toList());
        if (!normalApprovedIds.isEmpty()) {
            List<JobPositionsEntity> jobPositionsEntities = positionsRepository.findByRequisitionIdIn(normalApprovedIds);
            jobPositionsEntities.forEach(p -> p.setPositionStatus(PositionStatus.ACTIVE));
            positionsRepository.saveAll(jobPositionsEntities);
            try {
                embeddingBuilderService.generateEmbeddingForJobPoitions(normalApprovedIds);
            } catch (Exception e) {
                log.warn("Embedding generation failed for approved requisitions: {}", normalApprovedIds, e);
            }
        }

        // --- Edit-draft path (approve the draft only; no position activation, no publish) ---
        List<JobRequisitionsEntity> savedEditModeEntities = new ArrayList<>();
        for (JobRequisitionsEntity entity : editModeReqs) {
            JobRequisitionEditRequestEntity draft = jobRequisitionEditRequestRepository
                    .findTopByParentRequisitionIdAndIsActiveTrueAndRequisitionStatusInOrderByCreatedDateDesc(
                            entity.getId(), ACTIVE_DRAFT_STATUSES)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No active draft found for requisition: " + entity.getId()));

            if (draftRequiredCurrentStatus != null && draft.getRequisitionStatus() != draftRequiredCurrentStatus) {
                throw new CommonException(
                    String.format("Cannot process draft. Draft must be in '%s' state. Found '%s'.",
                    draftRequiredCurrentStatus, draft.getRequisitionStatus()));
            }
            draft.setRequisitionStatus(draftTargetStatus);
            if (jobPostingRequestModel.getComments() != null) {
                draft.setRequisitionComments(jobPostingRequestModel.getComments());
            }
            if (draftTargetStatus == RequisitionEditStatus.APPROVED) {
                draft.setApprovedAt(LocalDateTime.now());
            }
            jobRequisitionEditRequestRepository.save(draft);
            workflowEntities.add(createWorkflowEntityForDraft(draft));
            savedEditModeEntities.add(entity);
        }

        workflowApprovalEntityRepository.saveAll(workflowEntities);

        // Send email to next approver level for all (normal + edit-mode) when L1 approves.
        List<JobRequisitionsEntity> allSaved = new ArrayList<>();
        allSaved.addAll(savedNormalEntities);
        allSaved.addAll(savedEditModeEntities);
        if (normalTargetStatus != null && normalTargetStatus.equals(RequisitionStatus.L2_PENDING) && !allSaved.isEmpty()) {
            mailSenderHelper.sendRequisitionsApprovalMail(allSaved, RequisitionApproversEntity.ApproverRole.L2);
        }

        return allSaved.stream()
                .map(e -> toRequisitionWithDraftResponse(jobRequisitionMapper.toDTO(e)))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<JobRequisitionWithDraftResponseModel> submitForApproval(JobPostingRequestModel jobPostingRequestModel) throws MessagingException, IOException {
        List<UUID> reqIds = jobPostingRequestModel.getJobRequisitionIds();
        List<JobRequisitionsEntity> allEntities = jobRequisitionsRepository.findAllByIdIn(reqIds);

        // Split: normal NEW/rejected reqs vs. APPROVED reqs with an open edit draft.
        List<JobRequisitionsEntity> normalReqs = allEntities.stream()
                .filter(e -> !Boolean.TRUE.equals(e.getIsInEditMode()))
                .collect(Collectors.toList());
        List<JobRequisitionsEntity> editModeReqs = allEntities.stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsInEditMode()))
                .collect(Collectors.toList());

        List<WorkflowApprovalEntity> workflowEntities = new ArrayList<>();
        List<JobRequisitionsEntity> savedEntities = new ArrayList<>();

        // --- Normal requisition path ---
        for (JobRequisitionsEntity entity : normalReqs) {
            if (entity.getRequisitionStatus() != RequisitionStatus.NEW
                    && entity.getRequisitionStatus() != RequisitionStatus.L1_REJECTED
                    && entity.getRequisitionStatus() != RequisitionStatus.L2_REJECTED) {
                throw new IllegalStateException(
                        String.format("Cannot submit requisition. Must be in one of [%s, %s, %s] state. Found '%s'.",
                                RequisitionStatus.NEW, RequisitionStatus.L1_REJECTED,
                                RequisitionStatus.L2_REJECTED, entity.getRequisitionStatus()));
            }
            JobRequisitionsEntity oldEntity = entity.toBuilder().build();
            entity.setRequisitionStatus(RequisitionStatus.L1_PENDING);
            entity.setRequisitionComments(jobPostingRequestModel.getComments());
            workflowEntities.add(createWorkflowEntityCandidateApplication(entity));
            savedEntities.add(jobRequisitionsRepository.saveWithAudit(oldEntity, entity));
        }

        // --- Edit-draft path: submit the open draft for L1 review ---
        for (JobRequisitionsEntity entity : editModeReqs) {
            JobRequisitionEditRequestEntity draft = jobRequisitionEditRequestRepository
                    .findTopByParentRequisitionIdAndIsActiveTrueAndRequisitionStatusInOrderByCreatedDateDesc(
                            entity.getId(), ACTIVE_DRAFT_STATUSES)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No active draft found for requisition: " + entity.getId()));

            if (draft.getRequisitionStatus() != RequisitionEditStatus.DRAFT) {
                throw new CommonException(
                        String.format("Cannot submit draft. Draft must be in DRAFT state to submit for approval. Found '%s'.",
                                draft.getRequisitionStatus()));
            }
            draft.setRequisitionStatus(RequisitionEditStatus.L1_PENDING);
            draft.setSubmittedAt(LocalDateTime.now());
            // Only carry a comment if the recruiter explicitly provides one at submission time.
            // Clearing here prevents stale rejection comments from appearing on the new L1_PENDING row.
            draft.setRequisitionComments(
                    jobPostingRequestModel.getComments() != null ? jobPostingRequestModel.getComments() : null);
            jobRequisitionEditRequestRepository.save(draft);
            workflowEntities.add(createWorkflowEntityForDraft(draft));
            savedEntities.add(entity);
        }

        workflowApprovalEntityRepository.saveAll(workflowEntities);
        if (!savedEntities.isEmpty()) {
            mailSenderHelper.sendRequisitionsApprovalMail(savedEntities, RequisitionApproversEntity.ApproverRole.L1);
        }

        return savedEntities.stream()
                .map(e -> toRequisitionWithDraftResponse(jobRequisitionMapper.toDTO(e)))
                .collect(Collectors.toList());
    }


    public List<JobRequisitionsDTO> getApprovedRequisitionsByName(String searchText) {
        LocalDate today = LocalDate.now();
        String trimmedSearch = (searchText == null) ? null : searchText.trim();
        Set<UUID> requisitionIds=null;
        List<JobRequisitionsEntity> entities;
        boolean isCommitteeMember=securityUtils.getCurrentUserRole().equals(UserRole.COMMITTEE_MEMBER.getValue());
        if(isCommitteeMember){
            UUID currentUserId=securityUtils.getCurrentUserId();
            Set<UUID> panelIds = interviewPanelMembersRepository.findAllByPanelMember_Id(currentUserId).stream()
                                .map(member -> member.getPanel().getId())
                                .collect(Collectors.toSet());
            if (panelIds.isEmpty()) {
                return Collections.emptyList();
            }
            List<PositionPanelEntity> activePanels = positionPanelRepository.findActivePanels(panelIds, today,List.of(PositionPanelStatus.APPROVED));
            activePanels=activePanels.stream().filter(p->!p.getInterviewPanel().getCommittee().getCommitteeName().equals(AppConstants.INTERVIEW_COMMITTEE_NAME)).toList();
            requisitionIds = activePanels.stream()
                            .map(PositionPanelEntity::getJobPosition)
                            .map(JobPositionsEntity::getRequisitionId)
                            .collect(Collectors.toSet());

            if (requisitionIds.isEmpty()) {
                return Collections.emptyList();
            }
        }
        Specification<JobRequisitionsEntity> spec = buildSpecification(today, trimmedSearch, requisitionIds);
        entities = jobRequisitionsRepository.findAll(spec);
        return jobRequisitionMapper.toDTOs(entities);
    }

    public Specification<JobRequisitionsEntity> buildSpecification(LocalDate today, String searchText, Set<UUID> requisitionIds){
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            //Get approved requisitions
            predicates.add(cb.equal(root.get("requisitionStatus"), RequisitionStatus.APPROVED));
            // Filter it by reqIds if user is committee member
            if (requisitionIds != null && !requisitionIds.isEmpty()) {
                predicates.add(root.get("id").in(requisitionIds));
            }
            // search filter only for reqTitle
            if (searchText != null && !searchText.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("requisitionTitle")), "%" + searchText.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }


    public WorkflowApprovalEntity createWorkflowEntityCandidateApplication(JobRequisitionsEntity savedEntity){
        String approverRole = securityUtils.getCurrentUserRole();
        return WorkflowApprovalEntity.builder()
                .entityId(savedEntity.getId())
                .entityType(JobRequisitionsEntity.ENTITY_TYPE)
                .stepNumber(1) // Assuming step 1 for now
                .approverRole(approverRole)
                .approverId(securityUtils.getCurrentUserId())
                .action(DBConstants.WORKFLOW_ACTION_UPDATE)
                .actionDate(LocalDateTime.now())
                .comments(savedEntity.getRequisitionComments())
                .status(savedEntity.getRequisitionStatus().toString())
                .build();
    }

    private WorkflowApprovalEntity createWorkflowEntityForDraft(JobRequisitionEditRequestEntity draft) {
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

    public List<JobRequisitionsDTO> submitForApprovalOld(JobPostingRequestModel jobPostingRequestModel) {
        List<UUID> reqIds=jobPostingRequestModel.getJobRequisitionIds();

        List<JobRequisitionsEntity> jobRequisitionsEntities=jobRequisitionsRepository.findAllByIdIn(jobPostingRequestModel.getJobRequisitionIds());

        if (jobRequisitionsEntities.size() != reqIds.size()) {
            List<UUID> foundIds = jobRequisitionsEntities.stream()
                    .map(JobRequisitionsEntity::getId)
                    .toList();

            List<UUID> missingIds = reqIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            throw new CommonException("Job Requisitions not found for IDs: " + missingIds);
        }

        List<JobRequisitionsEntity> jobRequisitionsEntityList=new ArrayList<>();
        for(int i=0;i<jobRequisitionsEntities.size();i++){
            JobRequisitionsEntity oldEntity=jobRequisitionsEntities.get(i).toBuilder().build();
            JobRequisitionsEntity newEntity=jobRequisitionsEntities.get(i);
            newEntity.setRequisitionStatus(jobPostingRequestModel.getPostingStatus());
            jobRequisitionsEntityList.add(jobRequisitionsRepository.saveWithAudit(oldEntity,newEntity));
        }
        List<JobPositionsEntity> jobPositionsEntities=positionsRepository.findByRequisitionIdIn(jobPostingRequestModel.getJobRequisitionIds());
        List<JobPositionsEntity> updatedEntities=jobPositionsEntities.stream().map((jobPositionsEntity)->{
            jobPositionsEntity.setPositionStatus(PositionStatus.ACTIVE);
            return jobPositionsEntity;
        }).toList();
        positionsRepository.saveAll(updatedEntities);
        try{
            embeddingBuilderService.generateEmbeddingForJobPoitions(reqIds);
        } catch (Exception e) {
            // log
        }

        return jobRequisitionMapper.toDTOs(jobRequisitionsEntityList);
    }

    public List<Integer> getYears() {
        return jobRequisitionsRepository.getYears().stream().map(LocalDate::getYear).distinct().toList();
    }

    private JobRequisitionWithDraftResponseModel toRequisitionWithDraftResponse(JobRequisitionsDTO dto) {
        JobRequisitionWithDraftResponseModel response = new JobRequisitionWithDraftResponseModel();
        response.setId(dto.getId());
        response.setRequisitionTitle(dto.getRequisitionTitle());
        response.setRequisitionDescription(dto.getRequisitionDescription());
        response.setStartDate(dto.getStartDate());
        response.setEndDate(dto.getEndDate());
        response.setRequisitionStatus(dto.getRequisitionStatus());
        response.setRequisitionComments(dto.getRequisitionComments());
        response.setRequisitionCode(dto.getRequisitionCode());
        response.setDepartmentCount(dto.getDepartmentCount());
        response.setPositionCount(dto.getPositionCount());
        response.setVacancyCount(dto.getVacancyCount());
        response.setHasDraftPositions(dto.isHasDraftPositions());
        response.setParentRequisitionId(dto.getParentRequisitionId());
        response.setIsReinitialized(dto.getIsReinitialized());
        response.setCutoffDate(dto.getCutoffDate());
        response.setIsHiringCompleted(dto.getIsHiringCompleted());
        response.setCreatedDate(dto.getCreatedDate());
        response.setModifiedDate(dto.getModifiedDate());
        response.setCreatedBy(dto.getCreatedBy());
        response.setModifiedBy(dto.getModifiedBy());
        response.setIsActive(dto.getIsActive());

        boolean inEditMode = isRequisitionInEditMode(dto.getId());
        response.setIsInEditMode(inEditMode);
        if (inEditMode) {
            response.setDraft(fetchActiveDraft(dto.getId()));
        } else {
            response.setDraft(null);
        }
        return response;
    }

    private boolean isRequisitionInEditMode(UUID requisitionId) {
        return jobRequisitionsRepository.findById(requisitionId)
                .map(req -> Boolean.TRUE.equals(req.getIsInEditMode()))
                .orElse(false);
    }

    private RequisitionEditDraftResponseModel fetchActiveDraft(UUID requisitionId) {
        return jobRequisitionEditRequestRepository
                .findTopByParentRequisitionIdAndIsActiveTrueAndRequisitionStatusInOrderByCreatedDateDesc(requisitionId, ACTIVE_DRAFT_STATUSES)
                .map(this::toDraftResponse)
                .orElse(null);
    }

    private RequisitionEditDraftResponseModel toDraftResponse(JobRequisitionEditRequestEntity draft) {
        List<RequisitionEditPositionResponseModel> positions = draft.getPositionEditRequests() == null
                ? Collections.emptyList()
                : draft.getPositionEditRequests().stream()
                .map(pos -> RequisitionEditPositionResponseModel.builder()
                        .positionEditRequestId(pos.getId())
                        .parentPositionId(pos.getParentPositionId())
                        .masterPositionId(pos.getMasterPositionId())
                        .deptId(pos.getDeptId())
                        .totalVacancies(pos.getTotalVacancies())
                        .isLocationWise(pos.getIsLocationWise())
                        .build())
                .collect(Collectors.toList());

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
                .submittedAt(draft.getSubmittedAt())
                .approvedAt(draft.getApprovedAt())
                .publishedAt(draft.getPublishedAt())
                .publishedBy(draft.getPublishedBy())
                .positions(positions)
                .build();
    }
}
