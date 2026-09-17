package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.common.service.FileStorageService;
import com.sentrifugo.rms.db.entity.CandidateEntity;
import com.sentrifugo.rms.db.entity.JobPositionEntity;
import com.sentrifugo.rms.db.enums.CandidateStatus;
import com.sentrifugo.rms.db.repository.CandidateRepository;
import com.sentrifugo.rms.db.repository.JobPositionRepository;
import com.sentrifugo.rms.db.repository.PositionTitleRepository;
import com.sentrifugo.rms.recruiterportal.dto.CandidateDTO;
import com.sentrifugo.rms.recruiterportal.dto.ShortlistDecisionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final JobPositionRepository jobPositionRepository;
    private final PositionTitleRepository positionTitleRepository;
    private final FileStorageService fileStorageService;

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

    private void validateContactFields(CandidateDTO dto) {
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

    /** SCL_25: shortlist decision is Yes / No / On Hold, not a single Shortlist button. */
    @Transactional
    public void decide(UUID id, ShortlistDecisionRequest.Decision decision) {
        CandidateEntity entity = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        if (entity.getStatus() != CandidateStatus.ADDED && entity.getStatus() != CandidateStatus.ON_HOLD) {
            throw new CommonException("Only candidates that are Applied or On Hold can have a shortlist decision recorded.");
        }
        entity.setStatus(switch (decision) {
            case SHORTLIST -> CandidateStatus.SHORTLISTED;
            case REJECT -> CandidateStatus.NOT_SHORTLISTED;
            case HOLD -> CandidateStatus.ON_HOLD;
        });
        candidateRepository.save(entity);
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
