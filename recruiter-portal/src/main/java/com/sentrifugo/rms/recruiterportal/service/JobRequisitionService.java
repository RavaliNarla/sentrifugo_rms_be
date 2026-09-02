package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.common.service.MailService;
import com.sentrifugo.rms.common.util.SecurityUtils;
import com.sentrifugo.rms.db.entity.JobRequisitionEntity;
import com.sentrifugo.rms.db.entity.RequisitionApproverEntity;
import com.sentrifugo.rms.db.entity.UserEntity;
import com.sentrifugo.rms.db.enums.ApproverRole;
import com.sentrifugo.rms.db.enums.RequisitionStatus;
import com.sentrifugo.rms.db.repository.JobPositionRepository;
import com.sentrifugo.rms.db.repository.JobRequisitionRepository;
import com.sentrifugo.rms.db.repository.RequisitionApproverRepository;
import com.sentrifugo.rms.db.repository.UserRepository;
import com.sentrifugo.rms.recruiterportal.dto.ApprovalActionRequest;
import com.sentrifugo.rms.recruiterportal.dto.JobRequisitionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final MailService mailService;

    @Transactional
    public JobRequisitionDTO create(JobRequisitionDTO dto) {
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
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setStartDate(dto.getStartDate());
        entity.setExpectedFulfilmentDate(dto.getExpectedFulfilmentDate());
        return toDto(jobRequisitionRepository.save(entity));
    }

    public JobRequisitionDTO getById(UUID id) {
        return toDto(jobRequisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found")));
    }

    public Page<JobRequisitionDTO> getAll(int page, int size) {
        return jobRequisitionRepository.findAllByOrderByCreatedDateDesc(PageRequest.of(page, size))
                .map(this::toDto);
    }

    public List<JobRequisitionDTO> getApprovedForDropdown() {
        return jobRequisitionRepository.findApprovedForDropdown(RequisitionStatus.APPROVED).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<JobRequisitionDTO> getForL1Approval() {
        return jobRequisitionRepository.findByStatusIn(List.of(
                RequisitionStatus.L1_PENDING, RequisitionStatus.L2_PENDING,
                RequisitionStatus.APPROVED, RequisitionStatus.L1_REJECTED, RequisitionStatus.L2_REJECTED
        )).stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<JobRequisitionDTO> getForL2Approval() {
        return jobRequisitionRepository.findByStatusIn(List.of(
                RequisitionStatus.L2_PENDING, RequisitionStatus.APPROVED, RequisitionStatus.L2_REJECTED
        )).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public void submitForApproval(List<UUID> requisitionIds) {
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
        }
        jobRequisitionRepository.saveAll(requisitions);
        notifyApprover(ApproverRole.L1, "Requisition(s) submitted for your L1 approval.");
    }

    @Transactional
    public void approveOrReject(ApprovalActionRequest request) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        RequisitionApproverEntity approver = requisitionApproverRepository.findByApproverId(currentUserId)
                .orElseThrow(() -> new CommonException("You are not configured as an L1/L2 approver."));

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
