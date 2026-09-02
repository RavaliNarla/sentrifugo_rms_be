package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.common.service.FileStorageService;
import com.sentrifugo.rms.common.service.MailService;
import com.sentrifugo.rms.common.service.PdfConverterService;
import com.sentrifugo.rms.db.entity.*;
import com.sentrifugo.rms.db.enums.CandidateStatus;
import com.sentrifugo.rms.db.enums.OfferStatus;
import com.sentrifugo.rms.db.repository.*;
import com.sentrifugo.rms.recruiterportal.dto.CandidateOfferDTO;
import com.sentrifugo.rms.recruiterportal.dto.GenerateOfferRequest;
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
    private final SpringTemplateEngine templateEngine;
    private final PdfConverterService pdfConverterService;
    private final FileStorageService fileStorageService;
    private final MailService mailService;

    @Value("${app.base.url}")
    private String appBaseUrl;

    @Value("${app.company.name:Sentrifugo}")
    private String companyName;

    @Transactional
    public List<CandidateOfferDTO> generateAndSendOffers(GenerateOfferRequest request) {
        OfferTemplateEntity template = offerTemplateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Offer template not found"));

        List<CandidateEntity> candidates = candidateRepository.findByIdIn(request.getCandidateIds());
        List<CandidateOfferDTO> results = new java.util.ArrayList<>();

        for (CandidateEntity candidate : candidates) {
            if (candidate.getStatus() != CandidateStatus.MOVED_TO_OFFER) {
                throw new CommonException("Candidate '" + candidate.getName() + "' is not in the Offer Pool.");
            }
            if (candidate.getSalary() == null) {
                throw new CommonException("Candidate '" + candidate.getName() + "' has no salary set.");
            }

            String html = renderOfferHtml(candidate, template, request.getAcceptBeforeDate());
            byte[] pdfBytes = pdfConverterService.convertHtmlStringToPdf(html);
            String fileName = "offer-" + candidate.getId() + ".pdf";
            String storedPath = fileStorageService.storeBytes(pdfBytes, OFFER_FOLDER, fileName);

            CandidateOfferEntity offer = candidateOfferRepository.findByCandidateId(candidate.getId())
                    .orElse(CandidateOfferEntity.builder().candidateId(candidate.getId()).build());
            offer.setTemplateId(template.getId());
            offer.setAcceptBeforeDate(request.getAcceptBeforeDate());
            offer.setOfferFileUrl(storedPath);
            offer.setStatus(OfferStatus.SENT);
            offer.setAcceptToken(UUID.randomUUID());
            offer.setSentDate(LocalDateTime.now());
            candidateOfferRepository.save(offer);

            sendOfferEmail(candidate, offer, pdfBytes);

            results.add(toDto(offer, candidate.getName()));
        }
        return results;
    }

    public CandidateOfferDTO getByCandidateId(UUID candidateId) {
        CandidateEntity candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        return candidateOfferRepository.findByCandidateId(candidateId)
                .map(o -> toDto(o, candidate.getName()))
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

    private String renderOfferHtml(CandidateEntity candidate, OfferTemplateEntity template, LocalDate acceptBeforeDate) {
        JobPositionEntity position = jobPositionRepository.findById(candidate.getPositionId())
                .orElseThrow(() -> new ResourceNotFoundException("Position not found"));

        Context context = new Context();
        context.setVariable("candidateName", candidate.getName());
        context.setVariable("candidateEmail", candidate.getEmail());
        context.setVariable("candidatePhone", candidate.getPhone());
        context.setVariable("positionTitle", positionTitleRepository.findById(position.getPositionTitleId()).map(e -> e.getName()).orElse("-"));
        context.setVariable("department", departmentRepository.findById(position.getDepartmentId()).map(e -> e.getName()).orElse("-"));
        context.setVariable("location", locationRepository.findById(position.getLocationId()).map(e -> e.getName()).orElse("-"));
        context.setVariable("salary", candidate.getSalary());
        context.setVariable("acceptBeforeDate", acceptBeforeDate.toString());
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

    private CandidateOfferDTO toDto(CandidateOfferEntity entity, String candidateName) {
        return CandidateOfferDTO.builder()
                .id(entity.getId())
                .candidateId(entity.getCandidateId())
                .candidateName(candidateName)
                .acceptBeforeDate(entity.getAcceptBeforeDate())
                .offerFileUrl(entity.getOfferFileUrl())
                .status(entity.getStatus().name())
                .build();
    }
}
