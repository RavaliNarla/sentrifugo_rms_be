package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.common.service.FileStorageService;
import com.sentrifugo.rms.common.service.MailService;
import com.sentrifugo.rms.common.service.NotificationService;
import com.sentrifugo.rms.common.service.PdfConverterService;
import com.sentrifugo.rms.common.util.SecurityUtils;
import com.sentrifugo.rms.db.entity.*;
import com.sentrifugo.rms.db.enums.ApproverRole;
import com.sentrifugo.rms.db.enums.CandidateStatus;
import com.sentrifugo.rms.db.enums.OfferStatus;
import com.sentrifugo.rms.db.repository.*;
import com.sentrifugo.rms.recruiterportal.dto.CandidateOfferDTO;
import com.sentrifugo.rms.recruiterportal.dto.GenerateOfferRequest;
import com.sentrifugo.rms.recruiterportal.dto.OfferApprovalActionRequest;
import com.sentrifugo.rms.recruiterportal.dto.OfferApprovalHistoryDTO;
import com.sentrifugo.rms.recruiterportal.dto.OfferPreviewRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.sentrifugo.rms.common.util.IstTime;
import com.sentrifugo.rms.recruiterportal.email.RmsEmailTemplates;

/**
 * Offer letter generation + the retained Offer-Letter approval workflow
 * (Section 12/16 of the requirements doc): Generate (draft) -> Submit for
 * Approval -> L1 -> L2 -> auto-emailed to the candidate only after L2 approves.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfferService {

    private static final String OFFER_FOLDER = "offers";

    private final CandidateRepository candidateRepository;
    private final JobPositionRepository jobPositionRepository;
    private final JobRequisitionRepository jobRequisitionRepository;
    private final PositionTitleRepository positionTitleRepository;
    private final DepartmentRepository departmentRepository;
    private final LocationRepository locationRepository;
    private final OfferTemplateRepository offerTemplateRepository;
    private final CandidateOfferRepository candidateOfferRepository;
    private final OfferApprovalHistoryRepository offerApprovalHistoryRepository;
    private final RequisitionApproverRepository requisitionApproverRepository;
    private final UserRepository userRepository;
    private final SpringTemplateEngine templateEngine;
    private final PdfConverterService pdfConverterService;
    private final FileStorageService fileStorageService;
    private final MailService mailService;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;
    private final RmsEmailTemplates emailTemplates;

    @Value("${app.base.url}")
    private String appBaseUrl;

    @Value("${app.company.name:Sagar Cements Limited}")
    private String companyName;

    /** SCL_41: only accepted/rejected offers are locked; everything else can be regenerated and resent. */
    private static final List<OfferStatus> TERMINAL = List.of(OfferStatus.ACCEPTED, OfferStatus.REJECTED);

    // An offer already submitted for L1/L2 approval can't be silently regenerated out from under
    // the approver - the recruiter must wait for a decision (or have it rejected) first.
    private static final List<OfferStatus> PENDING_APPROVAL = List.of(OfferStatus.L1_PENDING, OfferStatus.L2_PENDING);

    // Offer Approvals screen: statuses visible to each level, mirroring the Requisition Approvals
    // screen (getForL1Approval/getForL2Approval) - decided requests stay visible with their final
    // status instead of disappearing from the list once acted on.
    private static final List<OfferStatus> L1_VISIBLE_STATUSES = List.of(
            OfferStatus.L1_PENDING, OfferStatus.L2_PENDING, OfferStatus.SENT, OfferStatus.L1_REJECTED, OfferStatus.L2_REJECTED);
    private static final List<OfferStatus> L2_VISIBLE_STATUSES = List.of(
            OfferStatus.L2_PENDING, OfferStatus.SENT, OfferStatus.L2_REJECTED);

    @Transactional
    public List<CandidateOfferDTO> generateOffers(GenerateOfferRequest request) {
        OfferTemplateEntity template = offerTemplateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Offer template not found"));

        if (request.getAcceptBeforeDate().isBefore(IstTime.today().plusDays(1))) {
            throw new CommonException("Accept Before Date must be a future date.");
        }

        List<CandidateEntity> candidates = candidateRepository.findByIdIn(request.getCandidateIds());
        List<CandidateOfferDTO> results = new java.util.ArrayList<>();

        for (CandidateEntity candidate : candidates) {
            if (candidate.getStatus() != CandidateStatus.MOVED_TO_OFFER) {
                throw new CommonException("Candidate '" + candidate.getName() + "' is not in the Offer Pool.");
            }
            if (candidate.getAgreedCtc() == null) {
                throw new CommonException("Candidate '" + candidate.getName() + "' has no Agreed CTC set.");
            }

            Optional<CandidateOfferEntity> existing = candidateOfferRepository.findByCandidateId(candidate.getId());
            if (existing.isPresent()) {
                OfferStatus existingStatus = existing.get().getStatus();
                // SCL_41: block only after candidate has accepted or rejected
                if (TERMINAL.contains(existingStatus)) {
                    throw new CommonException("Candidate '" + candidate.getName() + "' has already "
                            + existingStatus.name().toLowerCase() + " the offer; a new version cannot be sent.");
                }
                if (PENDING_APPROVAL.contains(existingStatus)) {
                    throw new CommonException("Candidate '" + candidate.getName() + "'s offer is already submitted for "
                            + existingStatus.name().replace("_PENDING", "") + " approval; it cannot be regenerated until a decision is made.");
                }
            }

            String html = renderOfferHtml(candidate, template, request.getAcceptBeforeDate(), request.getJoiningDate());
            byte[] pdfBytes = pdfConverterService.convertHtmlStringToPdf(html);
            String fileName = "offer-" + candidate.getId() + ".pdf";
            String storedPath = fileStorageService.storeBytes(pdfBytes, OFFER_FOLDER, fileName);

            CandidateOfferEntity offer = existing.orElse(CandidateOfferEntity.builder().candidateId(candidate.getId()).build());
            // SCL_41: invalidate any previously emailed accept/reject link immediately on regenerate
            archiveAcceptToken(offer);
            offer.setTemplateId(template.getId());
            offer.setAcceptBeforeDate(request.getAcceptBeforeDate());
            offer.setJoiningDate(request.getJoiningDate());
            offer.setOfferFileUrl(storedPath);
            offer.setStatus(OfferStatus.GENERATED);
            offer.setApprovalComments(null);
            offer.setAcceptToken(null);
            offer.setSentDate(null);
            offer.setDecidedDate(null);
            candidateOfferRepository.save(offer);

            results.add(toDto(offer, candidate));
        }
        return results;
    }

    /** SCL_36: render the letter for review without saving/sending. Candidate optional (placeholders). */
    public String previewOffer(OfferPreviewRequest request) {
        OfferTemplateEntity template = offerTemplateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Offer template not found"));

        // Only use real dates when the UI actually supplied them; otherwise keep <> placeholders.
        LocalDate acceptBefore = request.getAcceptBeforeDate();
        LocalDate joining = request.getJoiningDate();

        if (request.getCandidateId() == null) {
            return renderOfferHtmlWithPlaceholders(template, acceptBefore, joining);
        }

        CandidateEntity candidate = candidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        // Candidate selected but dates still optional in preview - fall back to sample dates only for rendering.
        LocalDate accept = acceptBefore != null ? acceptBefore : IstTime.today().plusDays(3);
        LocalDate join = joining != null ? joining : IstTime.today().plusDays(14);
        return renderOfferHtml(candidate, template, accept, join);
    }

    @Transactional
    public void submitForApproval(List<UUID> candidateIds) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        String actorName = resolveUserName(currentUserId);

        List<CandidateOfferEntity> offers = candidateOfferRepository.findByCandidateIdIn(candidateIds);
        for (CandidateOfferEntity offer : offers) {
            if (offer.getStatus() != OfferStatus.GENERATED) {
                throw new CommonException("Offer is not in a state that can be submitted for approval.");
            }
            offer.setStatus(OfferStatus.L1_PENDING);
            recordHistory(offer.getId(), currentUserId, actorName, OfferStatus.L1_PENDING.name(), null);
        }
        candidateOfferRepository.saveAll(offers);
        notifyApproversBothLevels("Offer letter(s) pending approval");

        for (CandidateOfferEntity offer : offers) {
            CandidateEntity candidate = candidateRepository.findById(offer.getCandidateId()).orElse(null);
            if (candidate == null) {
                continue;
            }
            String positionName = jobPositionRepository.findById(candidate.getPositionId())
                    .map(p -> positionTitleRepository.findById(p.getPositionTitleId()).map(t -> t.getName()).orElse("position"))
                    .orElse("position");
            String reqCode = jobRequisitionRepository.findById(candidate.getRequisitionId())
                    .map(r -> r.getRequisitionCode() != null ? r.getRequisitionCode() : r.getTitle())
                    .orElse("requisition");
            notificationService.notifyAdminsAndRecruiters(
                    NotificationService.TYPE_OFFER_SUBMITTED,
                    "New offer letter submitted for approval — " + candidate.getName()
                            + " (" + positionName + ", " + reqCode + ").",
                    currentUserId);
        }
    }

    @Transactional
    public void approveOrReject(OfferApprovalActionRequest request) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        RequisitionApproverEntity approver = requisitionApproverRepository.findByApproverId(currentUserId)
                .orElseThrow(() -> new CommonException("You are not configured as an L1/L2 approver."));
        String actorName = resolveUserName(currentUserId);

        List<CandidateOfferEntity> offers = candidateOfferRepository.findByCandidateIdIn(request.getCandidateIds());
        boolean anyL1Approved = false;

        for (CandidateOfferEntity offer : offers) {
            if (approver.getApproverRole() == ApproverRole.L1) {
                if (offer.getStatus() != OfferStatus.L1_PENDING) {
                    throw new CommonException("This offer is not awaiting L1 approval.");
                }
                offer.setStatus(request.isApprove() ? OfferStatus.L2_PENDING : OfferStatus.L1_REJECTED);
                anyL1Approved = anyL1Approved || request.isApprove();
            } else {
                if (offer.getStatus() != OfferStatus.L2_PENDING) {
                    throw new CommonException("This offer is not awaiting L2 approval.");
                }
                if (request.isApprove()) {
                    sendApprovedOffer(offer);
                } else {
                    offer.setStatus(OfferStatus.L2_REJECTED);
                }
            }
            offer.setApprovalComments(request.getComments());
            recordHistory(offer.getId(), currentUserId, actorName, offer.getStatus().name(), request.getComments());
        }
        candidateOfferRepository.saveAll(offers);

        for (CandidateOfferEntity o : offers) {
            UUID submitter = o.getCreatedBy();
            String label = "Offer letter";
            if (approver.getApproverRole() == ApproverRole.L1) {
                if (o.getStatus() == OfferStatus.L2_PENDING) {
                    notifyUsersAboutDecision(ApproverRole.L2, submitter, label, "L1", true, "Awaiting your L2 approval.");
                } else if (o.getStatus() == OfferStatus.L1_REJECTED) {
                    notifyUsersAboutDecision(null, submitter, label, "L1", false, "The offer was rejected at L1.");
                }
            } else if (o.getStatus() == OfferStatus.L2_REJECTED) {
                notifyUsersAboutDecision(null, submitter, label, "L2", false, "The offer was rejected at L2.");
            } else if (o.getStatus() == OfferStatus.SENT) {
                notifyUsersAboutDecision(null, submitter, label, "L2", true, "The offer was approved and emailed to the candidate.");
            }
        }
    }

    /** Approval history for the Offer Approvals history modal. */
    public List<OfferApprovalHistoryDTO> getApprovalHistory(UUID offerId) {
        if (!candidateOfferRepository.existsById(offerId)) {
            throw new ResourceNotFoundException("Offer not found");
        }
        return offerApprovalHistoryRepository.findByOfferIdOrderByCreatedDateDesc(offerId).stream()
                .map(h -> OfferApprovalHistoryDTO.builder()
                        .id(h.getId())
                        .approverName(h.getApproverName())
                        .approvalDate(h.getOccurredAt())
                        .status(h.getStatus())
                        .comments(h.getComments())
                        .build())
                .collect(Collectors.toList());
    }

    private void recordHistory(UUID offerId, UUID actorId, String actorName, String status, String comments) {
        offerApprovalHistoryRepository.save(OfferApprovalHistoryEntity.builder()
                .offerId(offerId)
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

    /** Candidate-facing send: after L2 approval. Rotates accept token so prior email links stop working. */
    private void sendApprovedOffer(CandidateOfferEntity offer) {
        CandidateEntity candidate = candidateRepository.findById(offer.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        archiveAcceptToken(offer);
        offer.setStatus(OfferStatus.SENT);
        offer.setAcceptToken(UUID.randomUUID());
        offer.setSentDate(IstTime.now());
        offer.setDecidedDate(null);

        try {
            byte[] pdfBytes = fileStorageService.load(offer.getOfferFileUrl()).getInputStream().readAllBytes();
            sendOfferEmail(candidate, offer, pdfBytes);
        } catch (Exception e) {
            log.warn("Failed to send offer email: {}", e.getMessage());
        }
    }

    private void archiveAcceptToken(CandidateOfferEntity offer) {
        if (offer.getAcceptToken() == null) {
            return;
        }
        String token = offer.getAcceptToken().toString();
        String existing = offer.getSupersededTokens();
        if (existing == null || existing.isBlank()) {
            offer.setSupersededTokens(token);
        } else if (!existing.contains(token)) {
            offer.setSupersededTokens(existing + "," + token);
        }
    }

    /** Drives the Offer Approvals screen: server-side candidate-name search/status/position-filter/pagination, newest first. */
    public Page<CandidateOfferDTO> searchPendingApprovals(String search, OfferStatus status, UUID positionId, int page, int size) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        RequisitionApproverEntity approver = requisitionApproverRepository.findByApproverId(currentUserId).orElse(null);
        if (approver == null) {
            return Page.empty();
        }
        List<OfferStatus> allowedStatuses = approver.getApproverRole() == ApproverRole.L1 ? L1_VISIBLE_STATUSES : L2_VISIBLE_STATUSES;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        return candidateOfferRepository.searchForApproval(allowedStatuses, status, positionId, search, pageRequest)
                .map(offer -> toDto(offer, candidateRepository.findById(offer.getCandidateId()).orElse(null)));
    }

    public CandidateOfferDTO getByCandidateId(UUID candidateId) {
        CandidateEntity candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        return candidateOfferRepository.findByCandidateId(candidateId)
                .map(o -> toDto(o, candidate))
                .orElse(null);
    }

    /** Accept/reject via the unauthenticated email link. Returns a simple status string for the confirmation page. */
    @Transactional
    public String decide(UUID token, boolean accept) {
        Optional<CandidateOfferEntity> offerOpt = candidateOfferRepository.findByAcceptToken(token);
        if (offerOpt.isEmpty()) {
            // SCL_41: prior email links after a newer send
            if (candidateOfferRepository.findBySupersededTokenContaining(token.toString()).isPresent()) {
                return "SUPERSEDED";
            }
            return "INVALID";
        }
        CandidateOfferEntity offer = offerOpt.get();

        if (offer.getStatus() == OfferStatus.ACCEPTED || offer.getStatus() == OfferStatus.REJECTED) {
            return offer.getStatus().name();
        }

        if (offer.getStatus() != OfferStatus.SENT && offer.getStatus() != OfferStatus.EXPIRED) {
            // Token matches a draft that was regenerated / not the live sent offer
            return "SUPERSEDED";
        }

        if (IstTime.today().isAfter(offer.getAcceptBeforeDate())) {
            offer.setStatus(OfferStatus.EXPIRED);
            candidateOfferRepository.save(offer);
            return "EXPIRED";
        }

        offer.setStatus(accept ? OfferStatus.ACCEPTED : OfferStatus.REJECTED);
        offer.setDecidedDate(IstTime.now());
        candidateOfferRepository.save(offer);
        sendOfferDecisionThankYou(offer, accept);
        return offer.getStatus().name();
    }

    private void sendOfferDecisionThankYou(CandidateOfferEntity offer, boolean accept) {
        try {
            CandidateEntity candidate = candidateRepository.findById(offer.getCandidateId()).orElse(null);
            if (candidate == null || candidate.getEmail() == null || candidate.getEmail().isBlank()) {
                return;
            }
            JobPositionEntity position = jobPositionRepository.findById(candidate.getPositionId()).orElse(null);
            String designation = position != null
                    ? positionTitleRepository.findById(position.getPositionTitleId()).map(x -> x.getName()).orElse("-")
                    : "-";
            String location = position != null
                    ? locationRepository.findById(position.getLocationId()).map(x -> x.getName()).orElse("-")
                    : "-";
            RmsEmailTemplates.BuiltEmail toCandidate = accept
                    ? emailTemplates.offerAccepted(candidate.getName(), designation, location, offer.getJoiningDate())
                    : emailTemplates.offerDeclined(candidate.getName(), designation);
            emailTemplates.sendAsync(candidate.getEmail(), toCandidate, true,
                    accept ? "offer-acceptance.pdf" : "offer-acknowledgement.pdf");

            UUID recruiterId = offer.getCreatedBy() != null ? offer.getCreatedBy() : offer.getModifiedBy();
            if (recruiterId != null) {
                userRepository.findById(recruiterId).ifPresent(u -> {
                    if (u.getEmail() != null && !u.getEmail().isBlank()) {
                        emailTemplates.sendAsync(u.getEmail(),
                                emailTemplates.recruiterOfferResponse(u.getName(), candidate.getName(), designation, accept));
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Failed to send offer decision thank-you email: {}", e.getMessage());
        }
    }

    private String renderOfferHtml(CandidateEntity candidate, OfferTemplateEntity template, LocalDate acceptBeforeDate, LocalDate joiningDate) {
        JobPositionEntity position = jobPositionRepository.findById(candidate.getPositionId())
                .orElseThrow(() -> new ResourceNotFoundException("Position not found"));

        Context context = new Context();
        context.setVariable("candidateName", candidate.getName());
        context.setVariable("candidateEmail", candidate.getEmail());
        context.setVariable("candidatePhone", candidate.getPhone());
        context.setVariable("positionTitle", positionTitleRepository.findById(position.getPositionTitleId()).map(e -> e.getName()).orElse("-"));
        context.setVariable("department", departmentRepository.findById(position.getDepartmentId()).map(e -> e.getName()).orElse("-"));
        context.setVariable("location", locationRepository.findById(position.getLocationId()).map(e -> e.getName()).orElse("-"));
        context.setVariable("salary", candidate.getAgreedCtc() != null ? candidate.getAgreedCtc() : candidate.getSalary());
        context.setVariable("acceptBeforeDate", acceptBeforeDate != null ? acceptBeforeDate.toString() : "-");
        context.setVariable("joiningDate", joiningDate != null ? joiningDate.toString() : "-");
        context.setVariable("companyName", companyName);

        String templateName = template.getFileName().replace(".html", "");
        return templateEngine.process("offer/" + templateName, context);
    }

    private String renderOfferHtmlWithPlaceholders(OfferTemplateEntity template, LocalDate acceptBeforeDate, LocalDate joiningDate) {
        Context context = new Context();
        context.setVariable("candidateName", "<Candidate_Name>");
        context.setVariable("candidateEmail", "<Candidate_Email>");
        context.setVariable("candidatePhone", "<Candidate_Phone>");
        context.setVariable("positionTitle", "<Position_Title>");
        context.setVariable("department", "<Department>");
        context.setVariable("location", "<Location>");
        context.setVariable("salary", "<Agreed_CTC>");
        context.setVariable("acceptBeforeDate", acceptBeforeDate != null ? acceptBeforeDate.toString() : "<Accept_Before_Date>");
        context.setVariable("joiningDate", joiningDate != null ? joiningDate.toString() : "<Joining_Date>");
        context.setVariable("companyName", companyName);

        String templateName = template.getFileName().replace(".html", "");
        return templateEngine.process("offer/" + templateName, context);
    }

    private void sendOfferEmail(CandidateEntity candidate, CandidateOfferEntity offer, byte[] pdfBytes) {
        try {
            String acceptUrl = appBaseUrl + "/api/v1/public/offers/" + offer.getAcceptToken() + "/accept";
            String rejectUrl = appBaseUrl + "/api/v1/public/offers/" + offer.getAcceptToken() + "/reject";
            JobPositionEntity position = jobPositionRepository.findById(candidate.getPositionId()).orElse(null);
            String designation = position != null
                    ? positionTitleRepository.findById(position.getPositionTitleId()).map(x -> x.getName()).orElse("-")
                    : "-";
            String location = position != null
                    ? locationRepository.findById(position.getLocationId()).map(x -> x.getName()).orElse("-")
                    : "-";
            RmsEmailTemplates.BuiltEmail email = emailTemplates.offerLetter(
                    candidate.getName(), designation, location,
                    offer.getJoiningDate(), offer.getAcceptBeforeDate(),
                    acceptUrl, rejectUrl, null);
            List<MailService.Attachment> attachments = List.of(
                    new MailService.Attachment("offer-letter.pdf", pdfBytes, "application/pdf"));
            emailTemplates.sendSyncWithAttachments(candidate.getEmail(), email, attachments);
        } catch (Exception ex) {
            log.warn("Failed to send offer email: {}", ex.getMessage());
        }
    }

    private void notifyApprover(ApproverRole role, String itemLabel, String level) {
        try {
            List<RequisitionApproverEntity> approvers = requisitionApproverRepository.findByApproverRole(role);
            for (RequisitionApproverEntity approver : approvers) {
                Optional<UserEntity> user = userRepository.findById(approver.getApproverId());
                user.ifPresent(u -> {
                    if (u.getEmail() != null) {
                        emailTemplates.sendAsync(u.getEmail(),
                                emailTemplates.approverSubmission(u.getName(), "Offer letter", itemLabel, level));
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Failed to notify {} offer approvers: {}", role, e.getMessage());
        }
    }

    private void notifyApproversBothLevels(String itemLabel) {
        notifyApprover(ApproverRole.L1, itemLabel, "L1");
        notifyApprover(ApproverRole.L2, itemLabel, "L2");
    }

    private void notifyUsersAboutDecision(ApproverRole nextRoleOrNull, UUID submitterId, String itemLabel,
                                          String decidedByLevel, boolean approved, String nextHint) {
        try {
            if (nextRoleOrNull != null) {
                for (RequisitionApproverEntity approver : requisitionApproverRepository.findByApproverRole(nextRoleOrNull)) {
                    userRepository.findById(approver.getApproverId()).ifPresent(u -> {
                        if (u.getEmail() != null) {
                            emailTemplates.sendAsync(u.getEmail(), emailTemplates.approvalDecisionNotice(
                                    u.getName(), "Offer letter", itemLabel, decidedByLevel, approved, nextHint));
                        }
                    });
                }
            }
            if (submitterId != null) {
                userRepository.findById(submitterId).ifPresent(u -> {
                    if (u.getEmail() != null) {
                        emailTemplates.sendAsync(u.getEmail(), emailTemplates.approvalDecisionNotice(
                                u.getName(), "Offer letter", itemLabel, decidedByLevel, approved, nextHint));
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Failed to send offer decision emails: {}", e.getMessage());
        }
    }


    private CandidateOfferDTO toDto(CandidateOfferEntity entity, CandidateEntity candidate) {
        String positionTitleName = null;
        String requisitionCode = null;
        if (candidate != null) {
            JobPositionEntity position = jobPositionRepository.findById(candidate.getPositionId()).orElse(null);
            if (position != null) {
                positionTitleName = positionTitleRepository.findById(position.getPositionTitleId()).map(PositionTitleEntity::getName).orElse(null);
                requisitionCode = jobRequisitionRepository.findById(position.getRequisitionId()).map(JobRequisitionEntity::getRequisitionCode).orElse(null);
            }
        }
        return CandidateOfferDTO.builder()
                .id(entity.getId())
                .candidateId(entity.getCandidateId())
                .candidateName(candidate != null ? candidate.getName() : null)
                .positionTitleName(positionTitleName)
                .requisitionCode(requisitionCode)
                .acceptBeforeDate(entity.getAcceptBeforeDate())
                .joiningDate(entity.getJoiningDate())
                .offerFileUrl(entity.getOfferFileUrl())
                .status(entity.getStatus().name())
                .approvalComments(entity.getApprovalComments())
                .build();
    }
}
