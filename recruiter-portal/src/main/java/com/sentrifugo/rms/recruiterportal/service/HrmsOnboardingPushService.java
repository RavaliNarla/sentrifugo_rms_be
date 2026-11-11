package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.common.service.FileStorageService;
import com.sentrifugo.rms.db.entity.CandidateEntity;
import com.sentrifugo.rms.db.entity.CandidateOfferEntity;
import com.sentrifugo.rms.db.entity.DepartmentEntity;
import com.sentrifugo.rms.db.entity.JobPositionEntity;
import com.sentrifugo.rms.db.entity.JobRequisitionEntity;
import com.sentrifugo.rms.db.entity.LocationEntity;
import com.sentrifugo.rms.db.entity.PositionTitleEntity;
import com.sentrifugo.rms.db.repository.CandidateOfferRepository;
import com.sentrifugo.rms.db.repository.CandidateRepository;
import com.sentrifugo.rms.db.repository.DepartmentRepository;
import com.sentrifugo.rms.db.repository.JobPositionRepository;
import com.sentrifugo.rms.db.repository.JobRequisitionRepository;
import com.sentrifugo.rms.db.repository.LocationRepository;
import com.sentrifugo.rms.db.repository.PositionTitleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

/**
 * Pushes an ACCEPTED offer's candidate into HRMS onboarding using the
 * recruiter's live HRMS Bearer token ({@code X-Hrms-Authorization}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrmsOnboardingPushService {

    private final HrmsOnboardingClient hrmsOnboardingClient;
    private final CandidateRepository candidateRepository;
    private final CandidateOfferRepository candidateOfferRepository;
    private final JobPositionRepository jobPositionRepository;
    private final JobRequisitionRepository jobRequisitionRepository;
    private final DepartmentRepository departmentRepository;
    private final LocationRepository locationRepository;
    private final PositionTitleRepository positionTitleRepository;
    private final FileStorageService fileStorageService;

    /**
     * Uploads resume then creates onboarding. Returns HRMS onboarding id.
     */
    public String pushOfferAccepted(UUID offerId, String hrmsAuthorization) {
        if (hrmsAuthorization == null || hrmsAuthorization.isBlank()) {
            throw new CommonException("HRMS session is required. Re-enter Recruitment from HRMS and try again.");
        }
        CandidateOfferEntity offer = candidateOfferRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));
        CandidateEntity candidate = candidateRepository.findById(offer.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        if (candidate.getResumeUrl() == null || candidate.getResumeUrl().isBlank()) {
            throw new CommonException("Candidate '" + candidate.getName() + "' has no resume on file.");
        }

        JobPositionEntity position = jobPositionRepository.findById(candidate.getPositionId()).orElse(null);
        JobRequisitionEntity requisition = jobRequisitionRepository.findById(candidate.getRequisitionId()).orElse(null);

        String requisitionCode = requisition != null && requisition.getRequisitionCode() != null
                ? requisition.getRequisitionCode()
                : (requisition != null ? requisition.getTitle() : null);

        String departmentHrmsId = firstNonBlank(
                position != null ? position.getDepartmentHrmsId() : null,
                position != null
                        ? departmentRepository.findById(position.getDepartmentId()).map(DepartmentEntity::getHrmsId).orElse(null)
                        : null);
        String businessUnitHrmsId = firstNonBlank(
                position != null ? position.getLocationHrmsId() : null,
                position != null
                        ? locationRepository.findById(position.getLocationId()).map(LocationEntity::getHrmsId).orElse(null)
                        : null);
        String designationHrmsId = firstNonBlank(
                position != null ? position.getPositionTitleHrmsId() : null,
                position != null
                        ? positionTitleRepository.findById(position.getPositionTitleId()).map(PositionTitleEntity::getHrmsId).orElse(null)
                        : null);

        try {
            ResumeFile resume = loadResume(candidate.getResumeUrl());
            String assetId = hrmsOnboardingClient.uploadResume(
                    hrmsAuthorization, resume.bytes(), resume.fileName(), resume.contentType());
            String onboardingId = hrmsOnboardingClient.createOnboarding(hrmsAuthorization,
                    new HrmsOnboardingClient.OnboardingPayload(
                            candidate.getFirstName(),
                            candidate.getMiddleName(),
                            candidate.getLastName(),
                            candidate.getDateOfBirth(),
                            candidate.getGenderHrmsId(),
                            candidate.getEmail(),
                            candidate.getPhone(),
                            requisitionCode,
                            businessUnitHrmsId,
                            departmentHrmsId,
                            designationHrmsId,
                            offer.getJoiningDate(),
                            assetId
                    ));
            log.info("HRMS onboarding created for candidate {} (offer {}): onboardingId={}, resumeAssetId={}",
                    candidate.getId(), offerId, onboardingId, assetId);
            return onboardingId;
        } catch (CommonException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("HRMS onboarding push failed for offer {}: {}", offerId, ex.getMessage(), ex);
            throw new CommonException("Failed to send '" + candidate.getName() + "' to HRMS onboarding: " + ex.getMessage());
        }
    }

    private ResumeFile loadResume(String resumeUrl) throws Exception {
        Resource resource = fileStorageService.load(resumeUrl);
        String fileName = resource.getFilename();
        if (fileName == null || fileName.isBlank()) {
            int slash = resumeUrl.lastIndexOf('/');
            fileName = slash >= 0 ? resumeUrl.substring(slash + 1) : resumeUrl;
        }
        try (InputStream in = resource.getInputStream()) {
            byte[] bytes = in.readAllBytes();
            return new ResumeFile(bytes, fileName, guessContentType(fileName));
        }
    }

    private static String guessContentType(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".doc")) {
            return "application/msword";
        }
        if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return "application/octet-stream";
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }

    private record ResumeFile(byte[] bytes, String fileName, String contentType) {}
}
