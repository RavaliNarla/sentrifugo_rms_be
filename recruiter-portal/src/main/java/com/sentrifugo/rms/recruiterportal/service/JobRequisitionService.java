package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.common.service.MailService;
import com.sentrifugo.rms.common.service.NotificationService;
import com.sentrifugo.rms.common.util.SecurityUtils;
import com.sentrifugo.rms.db.entity.DepartmentEntity;
import com.sentrifugo.rms.db.entity.JobPositionEntity;
import com.sentrifugo.rms.db.entity.JobRequisitionEntity;
import com.sentrifugo.rms.db.entity.LocationEntity;
import com.sentrifugo.rms.db.entity.PositionTitleEntity;
import com.sentrifugo.rms.db.entity.RequisitionApprovalHistoryEntity;
import com.sentrifugo.rms.db.entity.RequisitionApproverEntity;
import com.sentrifugo.rms.db.entity.UserEntity;
import com.sentrifugo.rms.db.enums.ApproverRole;
import com.sentrifugo.rms.db.enums.RequisitionStatus;
import com.sentrifugo.rms.db.repository.DepartmentRepository;
import com.sentrifugo.rms.db.repository.JobPositionRepository;
import com.sentrifugo.rms.db.repository.JobRequisitionRepository;
import com.sentrifugo.rms.db.repository.LocationRepository;
import com.sentrifugo.rms.db.repository.PositionTitleRepository;
import com.sentrifugo.rms.db.repository.RequisitionApprovalHistoryRepository;
import com.sentrifugo.rms.db.repository.RequisitionApproverRepository;
import com.sentrifugo.rms.db.repository.UserRepository;
import com.sentrifugo.rms.recruiterportal.dto.ApprovalActionRequest;
import com.sentrifugo.rms.recruiterportal.dto.JobRequisitionDTO;
import com.sentrifugo.rms.recruiterportal.dto.RequisitionApprovalHistoryDTO;
import com.sentrifugo.rms.recruiterportal.dto.RequisitionFilterOptionsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.sentrifugo.rms.common.util.IstTime;
import com.sentrifugo.rms.recruiterportal.email.RmsEmailTemplates;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobRequisitionService {

    private final JobRequisitionRepository jobRequisitionRepository;
    private final JobPositionRepository jobPositionRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionTitleRepository positionTitleRepository;
    private final LocationRepository locationRepository;
    private final RequisitionApproverRepository requisitionApproverRepository;
    private final RequisitionApprovalHistoryRepository approvalHistoryRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final MailService mailService;
    private final NotificationService notificationService;
    private final RmsEmailTemplates emailTemplates;

    @Transactional
    public JobRequisitionDTO create(JobRequisitionDTO dto) {
        validateDates(dto);
        UUID departmentId = blankToNull(dto.getDepartmentId());
        UUID locationId = blankToNull(dto.getLocationId());
        String deptCode = resolveDepartmentCode(departmentId);
        String locCode = resolveLocationCode(locationId);
        JobRequisitionEntity entity = JobRequisitionEntity.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .startDate(dto.getStartDate())
                .expectedFulfilmentDate(dto.getExpectedFulfilmentDate())
                .departmentId(departmentId)
                .locationId(locationId)
                .status(RequisitionStatus.NEW)
                .build();
        entity.setRequisitionCode(generateRequisitionCode(deptCode, locCode));
        JobRequisitionEntity saved = jobRequisitionRepository.save(entity);
        return toDto(saved);
    }

    public JobRequisitionDTO update(UUID id, JobRequisitionDTO dto) {
        JobRequisitionEntity entity = jobRequisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found"));
        if (entity.getStatus() != RequisitionStatus.NEW
                && entity.getStatus() != RequisitionStatus.L1_REJECTED
                && entity.getStatus() != RequisitionStatus.L2_REJECTED) {
            throw new CommonException("Only requisitions in NEW/REJECTED state can be edited.");
        }
        validateDates(dto);
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setStartDate(dto.getStartDate());
        entity.setExpectedFulfilmentDate(dto.getExpectedFulfilmentDate());
        // Department / location / requisition code stay as created (scope is fixed after create).
        return toDto(jobRequisitionRepository.save(entity));
    }

    /** SCL_02: Start Date and Expected Fulfilment Date must both be future dates (and fulfilment on/after start). */
    private void validateDates(JobRequisitionDTO dto) {
        LocalDate today = IstTime.today();
        if (dto.getStartDate() != null && dto.getStartDate().isBefore(today)) {
            throw new CommonException("Start Date must be a future date.");
        }
        if (dto.getExpectedFulfilmentDate() != null && dto.getExpectedFulfilmentDate().isBefore(today)) {
            throw new CommonException("Expected Fulfilment Date must be a future date.");
        }
        if (dto.getStartDate() != null && dto.getExpectedFulfilmentDate() != null
                && dto.getExpectedFulfilmentDate().isBefore(dto.getStartDate())) {
            throw new CommonException("Expected Fulfilment Date cannot be before the Start Date.");
        }
    }

    @Transactional
    public void delete(UUID id) {
        JobRequisitionEntity entity = jobRequisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found"));
        if (entity.getStatus() != RequisitionStatus.NEW) {
            throw new CommonException("Only requisitions in NEW status (not yet submitted/approved) can be deleted.");
        }
        jobPositionRepository.findByRequisitionId(id).forEach(jobPositionRepository::delete);
        jobRequisitionRepository.delete(entity);
    }

    public JobRequisitionDTO getById(UUID id) {
        return toDto(jobRequisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found")));
    }

    public Page<JobRequisitionDTO> getAll(int page, int size) {
        return jobRequisitionRepository.findAllByOrderByCreatedDateDesc(PageRequest.of(page, size))
                .map(this::toDto);
    }

    /** Drives the Job Postings screen: server-side search/filter/sort/pagination, newest first. */
    public Page<JobRequisitionDTO> search(String search, RequisitionStatus status, Integer yearFrom, Integer yearTo,
                                           Integer month, String jobTitle, String department, String location,
                                           int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        return jobRequisitionRepository.search(search, status, yearFrom, yearTo, month, jobTitle, department, location, pageRequest)
                .map(this::toDto);
    }

    /** Distinct values (across all requisitions/positions) for the Job Postings filter dropdowns. */
    public RequisitionFilterOptionsDTO getFilterOptions() {
        return RequisitionFilterOptionsDTO.builder()
                .years(jobRequisitionRepository.findDistinctStartYears())
                .jobTitles(jobPositionRepository.findDistinctPositionTitleNames())
                .departments(jobPositionRepository.findDistinctDepartmentNames())
                .locations(jobPositionRepository.findDistinctLocationNames())
                .build();
    }

    public List<JobRequisitionDTO> getApprovedForDropdown() {
        return jobRequisitionRepository.findApprovedForDropdown(RequisitionStatus.APPROVED).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Statuses visible at each approval level - decided requisitions stay visible with their final
    // status (mirrors Job Postings), not just ones still awaiting this approver's action.
    private static final List<RequisitionStatus> L1_VISIBLE_STATUSES = List.of(
            RequisitionStatus.L1_PENDING, RequisitionStatus.L2_PENDING,
            RequisitionStatus.APPROVED, RequisitionStatus.L1_REJECTED, RequisitionStatus.L2_REJECTED);
    private static final List<RequisitionStatus> L2_VISIBLE_STATUSES = List.of(
            RequisitionStatus.L2_PENDING, RequisitionStatus.APPROVED, RequisitionStatus.L2_REJECTED);

    /** Drives the Requisition Approvals screen: server-side search/status-filter/pagination, newest first. */
    public Page<JobRequisitionDTO> searchForApproval(boolean isL2, String search, RequisitionStatus status, int page, int size) {
        List<RequisitionStatus> allowedStatuses = isL2 ? L2_VISIBLE_STATUSES : L1_VISIBLE_STATUSES;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        return jobRequisitionRepository.searchForApproval(allowedStatuses, status, search, pageRequest).map(this::toDto);
    }

    /** SCL_53: chronological approval trail for the Job Postings history modal. */
    public List<RequisitionApprovalHistoryDTO> getApprovalHistory(UUID requisitionId) {
        if (!jobRequisitionRepository.existsById(requisitionId)) {
            throw new ResourceNotFoundException("Requisition not found");
        }
        return approvalHistoryRepository.findByRequisitionIdOrderByCreatedDateDesc(requisitionId).stream()
                .map(h -> RequisitionApprovalHistoryDTO.builder()
                        .id(h.getId())
                        .approverName(h.getApproverName())
                        .approvalDate(h.getOccurredAt())
                        .status(h.getStatus())
                        .comments(h.getComments())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void submitForApproval(List<UUID> requisitionIds) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        String actorName = resolveUserName(currentUserId);

        List<JobRequisitionEntity> requisitions = jobRequisitionRepository.findAllById(requisitionIds);
        for (JobRequisitionEntity requisition : requisitions) {
            if (requisition.getStatus() != RequisitionStatus.NEW
                    && requisition.getStatus() != RequisitionStatus.L1_REJECTED
                    && requisition.getStatus() != RequisitionStatus.L2_REJECTED) {
                throw new CommonException("Requisition '" + requisition.getTitle() + "' cannot be submitted from its current state.");
            }
            if (jobPositionRepository.findByRequisitionId(requisition.getId()).isEmpty()) {
                throw new CommonException("Requisition '" + requisition.getTitle() + "' has no positions. Add at least one position before submitting.");
            }
            // Stamp owning recruiter so reject/approve emails reach the submitter (not "first recruiter").
            if (requisition.getCreatedBy() == null) {
                requisition.setCreatedBy(currentUserId);
            }
            requisition.setStatus(RequisitionStatus.L1_PENDING);
            recordHistory(requisition.getId(), currentUserId, actorName, RequisitionStatus.L1_PENDING.name(), null);
        }
        jobRequisitionRepository.saveAll(requisitions);
        for (JobRequisitionEntity req : requisitions) {
            notifyApproversBothLevels(resolveRequisitionHiringDetails(req));
        }

        // Build in-app notifications after status save; emails are async and do not block this API.
        List<JobPositionEntity> allPositions = jobPositionRepository.findByRequisitionIdIn(requisitionIds);
        Map<UUID, Long> positionCounts = allPositions.stream()
                .collect(Collectors.groupingBy(JobPositionEntity::getRequisitionId, Collectors.counting()));
        for (JobRequisitionEntity requisition : requisitions) {
            long positionCount = positionCounts.getOrDefault(requisition.getId(), 0L);
            String code = requisition.getRequisitionCode() != null ? requisition.getRequisitionCode() : requisition.getTitle();
            notificationService.notifyAdminsAndRecruiters(
                    NotificationService.TYPE_REQUISITION_SUBMITTED,
                    "New requisition submitted for approval: " + code
                            + " — " + positionCount + " position" + (positionCount == 1 ? "" : "s") + ".",
                    currentUserId);
        }
    }

    @Transactional
    public void approveOrReject(ApprovalActionRequest request) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        RequisitionApproverEntity approver = requisitionApproverRepository.findByApproverId(currentUserId)
                .orElseThrow(() -> new CommonException("You are not configured as an L1/L2 approver."));
        String actorName = resolveUserName(currentUserId);

        List<JobRequisitionEntity> requisitions = jobRequisitionRepository.findAllById(request.getRequisitionIds());

        for (JobRequisitionEntity requisition : requisitions) {
            if (approver.getApproverRole() == ApproverRole.L1) {
                if (requisition.getStatus() != RequisitionStatus.L1_PENDING) {
                    throw new CommonException("Requisition '" + requisition.getTitle() + "' is not awaiting L1 approval.");
                }
                requisition.setStatus(request.isApprove() ? RequisitionStatus.L2_PENDING : RequisitionStatus.L1_REJECTED);
            } else {
                if (requisition.getStatus() != RequisitionStatus.L2_PENDING) {
                    throw new CommonException("Requisition '" + requisition.getTitle() + "' is not awaiting L2 approval.");
                }
                requisition.setStatus(request.isApprove() ? RequisitionStatus.APPROVED : RequisitionStatus.L2_REJECTED);
                if (request.isApprove()) {
                    jobPositionRepository.findByRequisitionId(requisition.getId()).forEach(position -> {
                        position.setStatus(com.sentrifugo.rms.db.enums.PositionStatus.ACTIVE);
                        jobPositionRepository.save(position);
                    });
                }
            }
            requisition.setComments(request.getComments());
            recordHistory(requisition.getId(), currentUserId, actorName, requisition.getStatus().name(), request.getComments());
        }
        jobRequisitionRepository.saveAll(requisitions);

        for (JobRequisitionEntity req : requisitions) {
            List<UUID> owners = resolveOwnerRecipients(req);
            RmsEmailTemplates.HiringDetails details = resolveRequisitionHiringDetails(req);
            String code = req.getRequisitionCode() != null ? req.getRequisitionCode() : req.getTitle();
            if (approver.getApproverRole() == ApproverRole.L1) {
                if (request.isApprove()) {
                    notifyUsersAboutDecision(ApproverRole.L2, owners, "L1", true, "Awaiting your L2 approval.", details);
                    notificationService.notifyAdminsAndRecruitersInApp(
                            NotificationService.TYPE_REQUISITION_APPROVED,
                            "Requisition approved at L1 — awaiting L2: " + code + ".",
                            currentUserId);
                } else {
                    notifyUsersAboutDecision(null, owners, "L1", false, "The requisition was rejected at L1.", details);
                    notificationService.notifyAdminsAndRecruitersInApp(
                            NotificationService.TYPE_REQUISITION_REJECTED,
                            "Requisition rejected at L1: " + code + ".",
                            currentUserId);
                }
            } else {
                notifyUsersAboutDecision(null, owners, "L2", request.isApprove(),
                        request.isApprove() ? "The requisition is now approved." : "The requisition was rejected at L2.",
                        details);
                if (request.isApprove()) {
                    notificationService.notifyAdminsAndRecruitersInApp(
                            NotificationService.TYPE_REQUISITION_APPROVED,
                            "Requisition approved: " + code + ".",
                            currentUserId);
                } else {
                    notificationService.notifyAdminsAndRecruitersInApp(
                            NotificationService.TYPE_REQUISITION_REJECTED,
                            "Requisition rejected at L2: " + code + ".",
                            currentUserId);
                }
            }
        }
    }

    private RmsEmailTemplates.HiringDetails resolveRequisitionHiringDetails(JobRequisitionEntity req) {
        String reqCode = req.getRequisitionCode() != null ? req.getRequisitionCode() : req.getTitle();
        List<JobPositionEntity> positions = jobPositionRepository.findByRequisitionId(req.getId());
        String positionNames = positions.stream()
                .map(p -> positionTitleRepository.findById(p.getPositionTitleId()).map(PositionTitleEntity::getName).orElse(null))
                .filter(n -> n != null && !n.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
        String locationNames = positions.stream()
                .map(p -> locationRepository.findById(p.getLocationId()).map(LocationEntity::getName).orElse(null))
                .filter(n -> n != null && !n.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
        return RmsEmailTemplates.HiringDetails.of(null, reqCode,
                positionNames.isBlank() ? null : positionNames,
                locationNames.isBlank() ? null : locationNames);
    }

    /**
     * Creator and/or submitter for ownership emails. When they differ, both are included (deduped).
     * Submitter = most recent L1_PENDING history actor.
     */
    private List<UUID> resolveOwnerRecipients(JobRequisitionEntity req) {
        LinkedHashSet<UUID> ids = new LinkedHashSet<>();
        if (req.getCreatedBy() != null) {
            ids.add(req.getCreatedBy());
        }
        approvalHistoryRepository.findByRequisitionIdOrderByCreatedDateDesc(req.getId()).stream()
                .filter(h -> RequisitionStatus.L1_PENDING.name().equals(h.getStatus()))
                .map(RequisitionApprovalHistoryEntity::getApproverId)
                .filter(id -> id != null)
                .findFirst()
                .ifPresent(ids::add);
        return List.copyOf(ids);
    }

    @Transactional
    public void markFulfilled(UUID id) {
        JobRequisitionEntity entity = jobRequisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found"));
        if (entity.getStatus() != RequisitionStatus.APPROVED) {
            throw new CommonException("Only approved requisitions can be marked as fulfilled.");
        }
        entity.setStatus(RequisitionStatus.FULFILLED);
        jobRequisitionRepository.save(entity);
    }

    /** Undo "Mark Fulfilled" - reopens the requisition back to APPROVED. */
    @Transactional
    public void unmarkFulfilled(UUID id) {
        JobRequisitionEntity entity = jobRequisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found"));
        if (entity.getStatus() != RequisitionStatus.FULFILLED) {
            throw new CommonException("Only fulfilled requisitions can be reopened.");
        }
        entity.setStatus(RequisitionStatus.APPROVED);
        jobRequisitionRepository.save(entity);
    }

    private void recordHistory(UUID requisitionId, UUID actorId, String actorName, String status, String comments) {
        approvalHistoryRepository.save(RequisitionApprovalHistoryEntity.builder()
                .requisitionId(requisitionId)
                .approverId(actorId)
                .approverName(actorName)
                .status(status)
                .comments(comments)
                .occurredAt(Instant.now())
                .build());
    }

    private String resolveUserName(UUID userId) {
        if (userId == null) {
            return "Unknown";
        }
        return userRepository.findById(userId).map(UserEntity::getName).orElse("Unknown");
    }

    private void notifyApprover(ApproverRole role, RmsEmailTemplates.HiringDetails details, String level) {
        try {
            List<RequisitionApproverEntity> approvers = requisitionApproverRepository.findByApproverRole(role);
            for (RequisitionApproverEntity approver : approvers) {
                Optional<UserEntity> user = userRepository.findById(approver.getApproverId());
                user.ifPresent(u -> {
                    if (u.getEmail() != null) {
                        emailTemplates.sendAsync(u.getEmail(),
                                emailTemplates.approverSubmission(u.getName(), "Requisition", level, details));
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Failed to notify {} requisition approvers: {}", role, e.getMessage());
        }
    }

    private void notifyApproversBothLevels(RmsEmailTemplates.HiringDetails details) {
        notifyApprover(ApproverRole.L1, details, "L1");
        notifyApprover(ApproverRole.L2, details, "L2");
    }

    private void notifyUsersAboutDecision(ApproverRole nextRoleOrNull, Collection<UUID> ownerIds,
                                          String decidedByLevel, boolean approved, String nextHint,
                                          RmsEmailTemplates.HiringDetails details) {
        try {
            Set<String> sentEmails = new HashSet<>();
            if (nextRoleOrNull != null) {
                for (RequisitionApproverEntity approver : requisitionApproverRepository.findByApproverRole(nextRoleOrNull)) {
                    userRepository.findById(approver.getApproverId()).ifPresent(u -> {
                        if (u.getEmail() != null && sentEmails.add(u.getEmail().trim().toLowerCase())) {
                            emailTemplates.sendAsync(u.getEmail(), emailTemplates.approvalDecisionNotice(
                                    u.getName(), "Requisition", decidedByLevel, approved, nextHint, details));
                        }
                    });
                }
            }
            if (ownerIds != null) {
                for (UUID ownerId : ownerIds) {
                    if (ownerId == null) {
                        continue;
                    }
                    userRepository.findById(ownerId).ifPresent(u -> {
                        if (u.getEmail() != null && sentEmails.add(u.getEmail().trim().toLowerCase())) {
                            emailTemplates.sendAsync(u.getEmail(), emailTemplates.approvalDecisionNotice(
                                    u.getName(), "Requisition", decidedByLevel, approved, nextHint, details));
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.warn("Failed to send requisition decision emails: {}", e.getMessage());
        }
    }


    private String generateRequisitionCode(String departmentCode, String locationCode) {
        Long seq = jobRequisitionRepository.nextRequisitionCodeSeq();
        String year = String.valueOf(IstTime.today().getYear());
        String seqPart = String.format("%05d", seq);
        StringBuilder sb = new StringBuilder("REQ");
        if (departmentCode != null && !departmentCode.isBlank()) {
            sb.append('-').append(departmentCode.trim().toUpperCase());
        }
        if (locationCode != null && !locationCode.isBlank()) {
            sb.append('-').append(locationCode.trim().toUpperCase());
        }
        sb.append('-').append(year).append('-').append(seqPart);
        return sb.toString();
    }

    private UUID blankToNull(UUID id) {
        return id;
    }

    private String resolveDepartmentCode(UUID departmentId) {
        if (departmentId == null) {
            return null;
        }
        DepartmentEntity dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new CommonException("Selected department was not found."));
        if (dept.getCode() == null || dept.getCode().isBlank()) {
            throw new CommonException("Selected department does not have a 3-character code. Update it in Admin → Departments.");
        }
        return dept.getCode().trim().toUpperCase();
    }

    private String resolveLocationCode(UUID locationId) {
        if (locationId == null) {
            return null;
        }
        LocationEntity loc = locationRepository.findById(locationId)
                .orElseThrow(() -> new CommonException("Selected location was not found."));
        if (loc.getCode() == null || loc.getCode().isBlank()) {
            throw new CommonException("Selected location does not have a 3-character code. Update it in Admin → Locations.");
        }
        return loc.getCode().trim().toUpperCase();
    }

    private JobRequisitionDTO toDto(JobRequisitionEntity entity) {
        String departmentName = null;
        String departmentCode = null;
        if (entity.getDepartmentId() != null) {
            DepartmentEntity dept = departmentRepository.findById(entity.getDepartmentId()).orElse(null);
            if (dept != null) {
                departmentName = dept.getName();
                departmentCode = dept.getCode();
            }
        }
        String locationName = null;
        String locationCode = null;
        if (entity.getLocationId() != null) {
            LocationEntity loc = locationRepository.findById(entity.getLocationId()).orElse(null);
            if (loc != null) {
                locationName = loc.getName();
                locationCode = loc.getCode();
            }
        }
        return JobRequisitionDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .startDate(entity.getStartDate())
                .expectedFulfilmentDate(entity.getExpectedFulfilmentDate())
                .status(entity.getStatus().name())
                .requisitionCode(entity.getRequisitionCode())
                .comments(entity.getComments())
                .departmentId(entity.getDepartmentId())
                .departmentName(departmentName)
                .departmentCode(departmentCode)
                .locationId(entity.getLocationId())
                .locationName(locationName)
                .locationCode(locationCode)
                .build();
    }
}
