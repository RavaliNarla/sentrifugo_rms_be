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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final JobPositionRepository jobPositionRepository;
    private final PositionTitleRepository positionTitleRepository;
    private final FileStorageService fileStorageService;

    private static final String RESUME_FOLDER = "resumes";
    private static final String ID_PROOF_FOLDER = "id-proofs";

    @Transactional
    public CandidateDTO add(CandidateDTO dto, MultipartFile resume, MultipartFile idProof) {
        JobPositionEntity position = jobPositionRepository.findById(dto.getPositionId())
                .orElseThrow(() -> new ResourceNotFoundException("Position not found"));

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

        return toDto(candidateRepository.save(entity));
    }

    public Page<CandidateDTO> search(UUID positionId, List<CandidateStatus> statuses, String searchText, int page, int size) {
        return candidateRepository.search(positionId, statuses, searchText, PageRequest.of(page, size))
                .map(this::toDto);
    }

    public CandidateDTO getById(UUID id) {
        return toDto(candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found")));
    }

    @Transactional
    public void shortlist(UUID id) {
        CandidateEntity entity = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        if (entity.getStatus() != CandidateStatus.ADDED) {
            throw new CommonException("Only candidates in ADDED status can be shortlisted.");
        }
        entity.setStatus(CandidateStatus.SHORTLISTED);
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
                .status(entity.getStatus().name())
                .finalScore(entity.getFinalScore())
                .salary(entity.getSalary())
                .build();
    }
}
