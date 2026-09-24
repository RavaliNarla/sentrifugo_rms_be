package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.common.service.FileStorageService;
import com.sentrifugo.rms.common.service.MailService;
import com.sentrifugo.rms.db.entity.CandidateEntity;
import com.sentrifugo.rms.db.entity.JobPositionEntity;
import com.sentrifugo.rms.db.enums.CandidateStatus;
import com.sentrifugo.rms.db.repository.CandidateRepository;
import com.sentrifugo.rms.db.repository.JobPositionRepository;
import com.sentrifugo.rms.db.repository.PositionTitleRepository;
import com.sentrifugo.rms.recruiterportal.dto.CandidateDTO;
import com.sentrifugo.rms.recruiterportal.dto.ShortlistDecisionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateService {

    /** Shortlist stage: first decision from ADDED, then only among these three outcomes. */
    private static final Set<CandidateStatus> SHORTLIST_DECISION_ALLOWED = EnumSet.of(
            CandidateStatus.ADDED,
            CandidateStatus.SHORTLISTED,
            CandidateStatus.REJECTED,
            CandidateStatus.ON_HOLD
    );

    private final CandidateRepository candidateRepository;
    private final JobPositionRepository jobPositionRepository;
    private final PositionTitleRepository positionTitleRepository;
    private final FileStorageService fileStorageService;
    private final MailService mailService;

    @Value("${app.company.name:Sagar Cement}")
    private String companyName;

    private static final String RESUME_FOLDER = "resumes";
    private static final String ID_PROOF_FOLDER = "id-proofs";
    private static final String PHOTO_FOLDER = "photos";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10}$");

    @Transactional
    public CandidateDTO add(CandidateDTO dto, MultipartFile resume, MultipartFile idProof, MultipartFile photo) {
        JobPositionEntity position = jobPositionRepository.findById(dto.getPositionId())
                .orElseThrow(() -> new ResourceNotFoundException("Position not found"));

        validateContactFields(dto);

        CandidateEntity entity = CandidateEntity.builder()
                .requisitionId(position.getRequisitionId())
                .positionId(position.getId())
                .name(dto.getName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .status(CandidateStatus.ADDED)
                .build();

        if (resume != null && !resume.isEmpty()) {
            entity.setResumeUrl(fileStorageService.store(resume, RESUME_FOLDER));
        }
        if (idProof != null && !idProof.isEmpty()) {
            entity.setIdProofUrl(fileStorageService.store(idProof, ID_PROOF_FOLDER));
        }
        if (photo != null && !photo.isEmpty()) {
            entity.setPhotoUrl(fileStorageService.store(photo, PHOTO_FOLDER));
        }

        return toDto(candidateRepository.save(entity));
    }

    /** SCL_42: edit is only allowed for newly-added candidates (before any workflow progress). */
    @Transactional
    public CandidateDTO update(UUID id, CandidateDTO dto, MultipartFile resume, MultipartFile idProof, MultipartFile photo) {
        CandidateEntity entity = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        if (entity.getStatus() != CandidateStatus.ADDED) {
            throw new CommonException("Only newly-added candidates can be edited.");
        }
        validateContactFields(dto);

        entity.setName(dto.getName());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        if (resume != null && !resume.isEmpty()) {
            entity.setResumeUrl(fileStorageService.store(resume, RESUME_FOLDER));
        }
        if (idProof != null && !idProof.isEmpty()) {
            entity.setIdProofUrl(fileStorageService.store(idProof, ID_PROOF_FOLDER));
        }
        if (photo != null && !photo.isEmpty()) {
            entity.setPhotoUrl(fileStorageService.store(photo, PHOTO_FOLDER));
        }
        return toDto(candidateRepository.save(entity));
    }

    /** SCL_42: delete is only allowed for newly-added candidates. */
    @Transactional
    public void delete(UUID id) {
        CandidateEntity entity = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        if (entity.getStatus() != CandidateStatus.ADDED) {
            throw new CommonException("Only newly-added candidates can be deleted.");
        }
        candidateRepository.delete(entity);
    }

    /**
     * Clears a single uploaded document (photo / resume / id-proof) while the candidate
     * is still editable (status = ADDED). Returns the updated candidate DTO.
     */
    @Transactional
    public CandidateDTO deleteDocument(UUID id, String documentType) {
        CandidateEntity entity = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        if (entity.getStatus() != CandidateStatus.ADDED) {
            throw new CommonException("Only newly-added candidates can have documents removed.");
        }

        String type = documentType == null ? "" : documentType.trim().toLowerCase().replace('_', '-');
        switch (type) {
            case "photo" -> {
                fileStorageService.deleteQuietly(entity.getPhotoUrl());
                entity.setPhotoUrl(null);
            }
            case "resume" -> {
                fileStorageService.deleteQuietly(entity.getResumeUrl());
                entity.setResumeUrl(null);
            }
            case "id-proof", "idproof" -> {
                fileStorageService.deleteQuietly(entity.getIdProofUrl());
                entity.setIdProofUrl(null);
            }
            default -> throw new CommonException("Unknown document type. Use photo, resume, or id-proof.");
        }
        return toDto(candidateRepository.save(entity));
    }

    private void validateContactFields(CandidateDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new CommonException("Name is required.");
        }
        if (dto.getEmail() == null || !EMAIL_PATTERN.matcher(dto.getEmail()).matches()) {
            throw new CommonException("Please enter a valid email address.");
        }
        if (dto.getPhone() == null || !PHONE_PATTERN.matcher(dto.getPhone()).matches()) {
            throw new CommonException("Please enter a valid 10-digit phone number.");
        }
    }

    public Page<CandidateDTO> search(UUID positionId, List<CandidateStatus> statuses, String searchText, int page, int size) {
        return candidateRepository.search(positionId, statuses, searchText, PageRequest.of(page, size))
                .map(this::toDto);
    }

    public CandidateDTO getById(UUID id) {
        return toDto(candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found")));
    }

    /**
     * Shortlist decision (SHORTLIST / REJECT / HOLD).
     * Only allowed while the candidate is still in the shortlist stage
     * (ADDED, SHORTLISTED, REJECTED, ON_HOLD) — not after scheduling / compensation / offer.
     */
    @Transactional
    public void decide(UUID id, ShortlistDecisionRequest.Decision decision) {
        CandidateEntity entity = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        if (!SHORTLIST_DECISION_ALLOWED.contains(entity.getStatus())) {
            throw new CommonException(
                    "Shortlist decisions are only allowed for candidates in Applied / SHORTLISTED / REJECTED / ON HOLD status. "
                            + "Current status: " + entity.getStatus() + ".");
        }

        CandidateStatus next = switch (decision) {
            case SHORTLIST -> CandidateStatus.SHORTLISTED;
            case REJECT -> CandidateStatus.REJECTED;
            case HOLD -> CandidateStatus.ON_HOLD;
        };
        entity.setStatus(next);
        candidateRepository.save(entity);

        queueShortlistStatusEmailAfterCommit(entity, decision);
    }

    private void queueShortlistStatusEmailAfterCommit(CandidateEntity entity, ShortlistDecisionRequest.Decision decision) {
        if (entity.getEmail() == null || entity.getEmail().isBlank()) {
            return;
        }
        String to = entity.getEmail();
        String name = entity.getName() != null ? entity.getName() : "Candidate";
        String positionTitle = jobPositionRepository.findById(entity.getPositionId())
                .map(p -> positionTitleRepository.findById(p.getPositionTitleId()).map(t -> t.getName()).orElse(null))
                .orElse(null);
        String positionPhrase = positionTitle != null
                ? "<b>" + escapeHtml(positionTitle) + "</b> at " + escapeHtml(companyName)
                : escapeHtml(companyName);

        String body;
        String subjectPrefix;
        switch (decision) {
            case SHORTLIST -> {
                subjectPrefix = "You have been shortlisted";
                body = "<p>Dear " + escapeHtml(name) + ",</p>"
                        + "<p>Thank you for your interest in " + positionPhrase + ".</p>"
                        + "<p>We are pleased to inform you that you have been <b>shortlisted</b> for the next stage of our recruitment process. Our team will contact you with further details shortly.</p>"
                        + "<p>Regards,<br/>" + escapeHtml(companyName) + " Recruitment Team</p>";
            }
            case HOLD -> {
                subjectPrefix = "Update on your application";
                body = "<p>Dear " + escapeHtml(name) + ",</p>"
                        + "<p>Thank you for your interest in " + positionPhrase + ".</p>"
                        + "<p>Your application is currently <b>on hold</b>. We will update you as soon as there is further progress.</p>"
                        + "<p>Regards,<br/>" + escapeHtml(companyName) + " Recruitment Team</p>";
            }
            case REJECT -> {
                subjectPrefix = "Update on your application";
                body = "<p>Dear " + escapeHtml(name) + ",</p>"
                        + "<p>Thank you for your interest in " + positionPhrase + ".</p>"
                        + "<p>After careful consideration, we will not be moving forward with your application at this time.</p>"
                        + "<p>We appreciate the time you invested and wish you the best in your career.</p>"
                        + "<p>Regards,<br/>" + escapeHtml(companyName) + " Recruitment Team</p>";
            }
            default -> {
                return;
            }
        }

        String subject = subjectPrefix + (positionTitle != null ? " — " + positionTitle : "");
        String html = body;

        Runnable send = () -> {
            try {
                mailService.sendHtmlEmailAsync(to, subject, html);
            } catch (Exception e) {
                log.warn("Failed to queue shortlist status email ({}) to {}: {}", decision, to, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    private static String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public String getResumeUrl(UUID id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"))
                .getResumeUrl();
    }

    public String getIdProofUrl(UUID id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"))
                .getIdProofUrl();
    }

    private CandidateDTO toDto(CandidateEntity entity) {
        String positionTitleName = jobPositionRepository.findById(entity.getPositionId())
                .map(p -> positionTitleRepository.findById(p.getPositionTitleId()).map(t -> t.getName()).orElse(null))
                .orElse(null);
        return CandidateDTO.builder()
                .id(entity.getId())
                .requisitionId(entity.getRequisitionId())
                .positionId(entity.getPositionId())
                .positionTitleName(positionTitleName)
                .name(entity.getName())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .resumeUrl(entity.getResumeUrl())
                .hasResume(entity.getResumeUrl() != null)
                .idProofUrl(entity.getIdProofUrl())
                .hasIdProof(entity.getIdProofUrl() != null)
                .photoUrl(entity.getPhotoUrl())
                .hasPhoto(entity.getPhotoUrl() != null)
                .status(entity.getStatus().name())
                .finalScore(entity.getFinalScore())
                .salary(entity.getSalary())
                .currentCtc(entity.getCurrentCtc())
                .expectedCtc(entity.getExpectedCtc())
                .fixedPay(entity.getFixedPay())
                .variablePay(entity.getVariablePay())
                .bonus(entity.getBonus())
                .compensationComments(entity.getCompensationComments())
                .agreedCtc(entity.getAgreedCtc())
                .build();
    }
}
