package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.common.service.MailService;
import com.sentrifugo.rms.common.util.SecurityUtils;
import com.sentrifugo.rms.db.entity.JobRequisitionEntity;
import com.sentrifugo.rms.db.entity.RequisitionApprovalHistoryEntity;
import com.sentrifugo.rms.db.entity.RequisitionApproverEntity;
import com.sentrifugo.rms.db.entity.UserEntity;
import com.sentrifugo.rms.db.enums.ApproverRole;
import com.sentrifugo.rms.db.enums.RequisitionStatus;
import com.sentrifugo.rms.db.repository.JobPositionRepository;
import com.sentrifugo.rms.db.repository.JobRequisitionRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobRequisitionService {

    private final JobRequisitionRepository jobRequisitionRepository;
    private final JobPositionRepository jobPositionRepository;
    private final RequisitionApproverRepository requisitionApproverRepository;
    private final RequisitionApprovalHistoryRepository approvalHistoryRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final MailService mailService;

    @Transactional
    public JobRequisitionDTO create(JobRequisitionDTO dto) {
        validateDates(dto);
        JobRequisitionEntity entity = JobRequisitionEntity.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .startDate(dto.getStartDate())
                .expectedFulfilmentDate(dto.getExpectedFulfilmentDate())
                .status(RequisitionStatus.NEW)
                .build();
        entity.setRequisitionCode(generateRequisitionCode());
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
        // Keep rejected status as-is after edit-save (do not reset to NEW).
        return toDto(jobRequisitionRepository.save(entity));
    }

    /** SCL_02: Start Date and Expected Fulfilment Date must both be future dates (and fulfilment on/after start). */
    private void validateDates(JobRequisitionDTO dto) {
        LocalDate today = LocalDate.now();
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
                        .approvalDate(h.getCreatedDate())
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
            requisition.setStatus(RequisitionStatus.L1_PENDING);
            recordHistory(requisition.getId(), currentUserId, actorName, RequisitionStatus.L1_PENDING.name(), null);
        }
        jobRequisitionRepository.saveAll(requisitions);
        notifyApprover(ApproverRole.L1, "Requisition(s) submitted for your L1 approval.");
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

        if (approver.getApproverRole() == ApproverRole.L1 && request.isApprove()) {
            notifyApprover(ApproverRole.L2, "Requisition(s) approved by L1, awaiting your L2 approval.");
        }
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
                .build());
    }

    private String resolveUserName(UUID userId) {
        if (userId == null) {
            return "Unknown";
        }
        return userRepository.findById(userId).map(UserEntity::getName).orElse("Unknown");
    }

    private void notifyApprover(ApproverRole role, String message) {
        try {
            List<RequisitionApproverEntity> approvers = requisitionApproverRepository.findByApproverRole(role);
            for (RequisitionApproverEntity approver : approvers) {
                Optional<UserEntity> user = userRepository.findById(approver.getApproverId());
                user.ifPresent(u -> mailService.sendHtmlEmail(u.getEmail(), "Requisition Approval Pending",
                        "<p>Hi " + u.getName() + ",</p><p>" + message + " Please log in to the Sentrifugo RMS Recruiter Portal to review.</p>"));
            }
        } catch (Exception e) {
            log.warn("Failed to send approver notification email: {}", e.getMessage());
        }
    }

    private String generateRequisitionCode() {
        Long seq = jobRequisitionRepository.nextRequisitionCodeSeq();
        return "REQ-" + LocalDate.now().getYear() + "-" + String.format("%05d", seq);
    }

    private JobRequisitionDTO toDto(JobRequisitionEntity entity) {
        return JobRequisitionDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .startDate(entity.getStartDate())
                .expectedFulfilmentDate(entity.getExpectedFulfilmentDate())
                .status(entity.getStatus().name())
                .requisitionCode(entity.getRequisitionCode())
                .comments(entity.getComments())
                .build();
    }
}
