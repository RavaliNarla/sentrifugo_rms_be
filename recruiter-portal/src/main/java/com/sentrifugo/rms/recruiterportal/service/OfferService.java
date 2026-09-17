package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.common.service.FileStorageService;
import com.sentrifugo.rms.common.service.MailService;
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
import com.sentrifugo.rms.recruiterportal.dto.OfferPreviewRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final PositionTitleRepository positionTitleRepository;
    private final DepartmentRepository departmentRepository;
    private final LocationRepository locationRepository;
    private final OfferTemplateRepository offerTemplateRepository;
    private final CandidateOfferRepository candidateOfferRepository;
    private final RequisitionApproverRepository requisitionApproverRepository;
    private final UserRepository userRepository;
    private final SpringTemplateEngine templateEngine;
    private final PdfConverterService pdfConverterService;
    private final FileStorageService fileStorageService;
    private final MailService mailService;
    private final SecurityUtils securityUtils;

    @Value("${app.base.url}")
    private String appBaseUrl;

    @Value("${app.company.name:Sentrifugo}")
    private String companyName;

    private static final List<OfferStatus> REGENERATABLE = List.of(OfferStatus.GENERATED, OfferStatus.L1_REJECTED, OfferStatus.L2_REJECTED);

    @Transactional
    public List<CandidateOfferDTO> generateOffers(GenerateOfferRequest request) {
        OfferTemplateEntity template = offerTemplateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Offer template not found"));

        if (request.getAcceptBeforeDate().isBefore(LocalDate.now().plusDays(1))) {
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
            if (existing.isPresent() && !REGENERATABLE.contains(existing.get().getStatus())) {
                // SCL_41: once an offer has been submitted/sent, the recruiter cannot generate/send it again.
                throw new CommonException("Candidate '" + candidate.getName() + "' already has an offer in progress (" + existing.get().getStatus() + ").");
            }

            String html = renderOfferHtml(candidate, template, request.getAcceptBeforeDate(), request.getJoiningDate());
            byte[] pdfBytes = pdfConverterService.convertHtmlStringToPdf(html);
            String fileName = "offer-" + candidate.getId() + ".pdf";
            String storedPath = fileStorageService.storeBytes(pdfBytes, OFFER_FOLDER, fileName);

            CandidateOfferEntity offer = existing.orElse(CandidateOfferEntity.builder().candidateId(candidate.getId()).build());
            offer.setTemplateId(template.getId());
            offer.setAcceptBeforeDate(request.getAcceptBeforeDate());
            offer.setJoiningDate(request.getJoiningDate());
            offer.setOfferFileUrl(storedPath);
            offer.setStatus(OfferStatus.GENERATED);
            offer.setApprovalComments(null);
            candidateOfferRepository.save(offer);

            results.add(toDto(offer, candidate));
        }
        return results;
    }

    /** SCL_36: render the letter for review without saving/sending anything. */
    public String previewOffer(OfferPreviewRequest request) {
        CandidateEntity candidate = candidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        OfferTemplateEntity template = offerTemplateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Offer template not found"));
        LocalDate acceptBefore = request.getAcceptBeforeDate() != null ? request.getAcceptBeforeDate() : LocalDate.now().plusDays(3);
        LocalDate joining = request.getJoiningDate() != null ? request.getJoiningDate() : LocalDate.now().plusDays(14);
        return renderOfferHtml(candidate, template, acceptBefore, joining);
    }

    @Transactional
    public void submitForApproval(List<UUID> candidateIds) {
        List<CandidateOfferEntity> offers = candidateOfferRepository.findByCandidateIdIn(candidateIds);
        for (CandidateOfferEntity offer : offers) {
            if (offer.getStatus() != OfferStatus.GENERATED) {
                throw new CommonException("Offer is not in a state that can be submitted for approval.");
            }
            offer.setStatus(OfferStatus.L1_PENDING);
        }
        candidateOfferRepository.saveAll(offers);
        notifyApprover(ApproverRole.L1, "Offer letter(s) submitted for your L1 approval.");
    }

    @Transactional
    public void approveOrReject(OfferApprovalActionRequest request) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        RequisitionApproverEntity approver = requisitionApproverRepository.findByApproverId(currentUserId)
                .orElseThrow(() -> new CommonException("You are not configured as an L1/L2 approver."));

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
        }
        candidateOfferRepository.saveAll(offers);

        if (approver.getApproverRole() == ApproverRole.L1 && anyL1Approved) {
            notifyApprover(ApproverRole.L2, "Offer letter(s) approved by L1, awaiting your L2 approval.");
        }
    }

    /** Candidate-facing send: only happens once, right after L2 approval. */
    private void sendApprovedOffer(CandidateOfferEntity offer) {
        CandidateEntity candidate = candidateRepository.findById(offer.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        offer.setStatus(OfferStatus.SENT);
        offer.setAcceptToken(UUID.randomUUID());
        offer.setSentDate(LocalDateTime.now());

        try {
            byte[] pdfBytes = fileStorageService.load(offer.getOfferFileUrl()).getInputStream().readAllBytes();
            sendOfferEmail(candidate, offer, pdfBytes);
        } catch (Exception e) {
            log.warn("Failed to send offer email: {}", e.getMessage());
        }
    }

    public List<CandidateOfferDTO> getPendingApprovals() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        RequisitionApproverEntity approver = requisitionApproverRepository.findByApproverId(currentUserId).orElse(null);
        if (approver == null) {
            return List.of();
        }
        OfferStatus status = approver.getApproverRole() == ApproverRole.L1 ? OfferStatus.L1_PENDING : OfferStatus.L2_PENDING;
        return candidateOfferRepository.findByStatus(status).stream()
                .map(offer -> toDto(offer, candidateRepository.findById(offer.getCandidateId()).orElse(null)))
                .collect(Collectors.toList());
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
            return "INVALID";
        }
        CandidateOfferEntity offer = offerOpt.get();

        if (offer.getStatus() == OfferStatus.ACCEPTED || offer.getStatus() == OfferStatus.REJECTED) {
            return offer.getStatus().name();
        }

        if (LocalDate.now().isAfter(offer.getAcceptBeforeDate())) {
            offer.setStatus(OfferStatus.EXPIRED);
            candidateOfferRepository.save(offer);
            return "EXPIRED";
        }

        offer.setStatus(accept ? OfferStatus.ACCEPTED : OfferStatus.REJECTED);
        offer.setDecidedDate(LocalDateTime.now());
        candidateOfferRepository.save(offer);
        return offer.getStatus().name();
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

    private void sendOfferEmail(CandidateEntity candidate, CandidateOfferEntity offer, byte[] pdfBytes) {
        try {
            String acceptUrl = appBaseUrl + "/api/v1/public/offers/" + offer.getAcceptToken() + "/accept";
            String rejectUrl = appBaseUrl + "/api/v1/public/offers/" + offer.getAcceptToken() + "/reject";

            String html = "<p>Dear " + candidate.getName() + ",</p>"
                    + "<p>Please find attached your offer letter. Kindly respond on or before <b>"
                    + offer.getAcceptBeforeDate() + "</b>.</p>"
                    + "<p><b>Joining Date:</b> " + (offer.getJoiningDate() != null ? offer.getJoiningDate() : "-") + "</p>"
                    + "<p style='margin-top:20px;'>"
                    + "<a href='" + acceptUrl + "' style='background:#208bbd;color:#fff;padding:10px 24px;text-decoration:none;border-radius:4px;margin-right:12px;'>Accept Offer</a>"
                    + "<a href='" + rejectUrl + "' style='background:#a20e37;color:#fff;padding:10px 24px;text-decoration:none;border-radius:4px;'>Reject Offer</a>"
                    + "</p>"
                    + "<p style='color:#888;font-size:12px;margin-top:16px;'>This link will expire after " + offer.getAcceptBeforeDate() + ".</p>";

            mailService.sendHtmlEmail(candidate.getEmail(), "Your Offer Letter", html, pdfBytes, "offer-letter.pdf");
        } catch (Exception e) {
            log.warn("Failed to send offer email: {}", e.getMessage());
        }
    }

    private void notifyApprover(ApproverRole role, String message) {
        try {
            List<RequisitionApproverEntity> approvers = requisitionApproverRepository.findByApproverRole(role);
            for (RequisitionApproverEntity approver : approvers) {
                Optional<UserEntity> user = userRepository.findById(approver.getApproverId());
                user.ifPresent(u -> mailService.sendHtmlEmail(u.getEmail(), "Offer Letter Approval Pending",
                        "<p>Hi " + u.getName() + ",</p><p>" + message + " Please log in to the Sentrifugo RMS Recruiter Portal to review.</p>"));
            }
        } catch (Exception e) {
            log.warn("Failed to send offer approver notification email: {}", e.getMessage());
        }
    }

    private CandidateOfferDTO toDto(CandidateOfferEntity entity, CandidateEntity candidate) {
        String positionTitleName = null;
        if (candidate != null) {
            positionTitleName = jobPositionRepository.findById(candidate.getPositionId())
                    .map(p -> positionTitleRepository.findById(p.getPositionTitleId()).map(t -> t.getName()).orElse(null))
                    .orElse(null);
        }
        return CandidateOfferDTO.builder()
                .id(entity.getId())
                .candidateId(entity.getCandidateId())
                .candidateName(candidate != null ? candidate.getName() : null)
                .positionTitleName(positionTitleName)
                .acceptBeforeDate(entity.getAcceptBeforeDate())
                .joiningDate(entity.getJoiningDate())
                .offerFileUrl(entity.getOfferFileUrl())
                .status(entity.getStatus().name())
                .approvalComments(entity.getApprovalComments())
                .build();
    }
}
