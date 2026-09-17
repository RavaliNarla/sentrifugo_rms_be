package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.db.entity.CandidateEntity;
import com.sentrifugo.rms.db.enums.CandidateStatus;
import com.sentrifugo.rms.db.repository.CandidateRepository;
import com.sentrifugo.rms.recruiterportal.dto.CandidateDTO;
import com.sentrifugo.rms.recruiterportal.dto.CompensationDetailsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Compensation Management (Section 15 of the requirements doc, renamed from "Compensation Pool"). */
@Service
@RequiredArgsConstructor
public class CompensationService {

    private final CandidateRepository candidateRepository;

    @Transactional
    public void moveToCompensation(List<UUID> candidateIds) {
        List<CandidateEntity> candidates = candidateRepository.findByIdIn(candidateIds);
        for (CandidateEntity candidate : candidates) {
            if (candidate.getStatus() != CandidateStatus.QUALIFIED) {
                throw new CommonException("Candidate '" + candidate.getName() + "' must be QUALIFIED to move to Compensation Management.");
            }
            candidate.setStatus(CandidateStatus.COMPENSATION_PENDING);
        }
        candidateRepository.saveAll(candidates);
    }

    @Transactional
    public void updateCompensationDetails(UUID candidateId, CompensationDetailsRequest request) {
        CandidateEntity candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        if (candidate.getStatus() != CandidateStatus.COMPENSATION_PENDING) {
            throw new CommonException("Candidate is not in Compensation Management.");
        }
        candidate.setCurrentCtc(request.getCurrentCtc());
        candidate.setExpectedCtc(request.getExpectedCtc());
        candidate.setFixedPay(request.getFixedPay());
        candidate.setVariablePay(request.getVariablePay());
        candidate.setBonus(request.getBonus());
        candidate.setCompensationComments(request.getCompensationComments());
        candidate.setAgreedCtc(request.getAgreedCtc());
        candidateRepository.save(candidate);
    }

    @Transactional
    public void moveToOffer(List<UUID> candidateIds) {
        List<CandidateEntity> candidates = candidateRepository.findByIdIn(candidateIds);
        for (CandidateEntity candidate : candidates) {
            if (candidate.getStatus() != CandidateStatus.COMPENSATION_PENDING) {
                throw new CommonException("Candidate '" + candidate.getName() + "' is not in Compensation Management.");
            }
            if (candidate.getAgreedCtc() == null) {
                throw new CommonException("Candidate '" + candidate.getName() + "' has no Agreed CTC entered.");
            }
            candidate.setStatus(CandidateStatus.MOVED_TO_OFFER);
        }
        candidateRepository.saveAll(candidates);
    }

    public Page<CandidateDTO> getCompensationPool(UUID positionId, String searchText, int page, int size) {
        Page<CandidateEntity> result = candidateRepository.search(positionId,
                List.of(CandidateStatus.COMPENSATION_PENDING), searchText, PageRequest.of(page, size));
        return result.map(this::toDto);
    }

    private CandidateDTO toDto(CandidateEntity entity) {
        return CandidateDTO.builder()
                .id(entity.getId())
                .requisitionId(entity.getRequisitionId())
                .positionId(entity.getPositionId())
                .name(entity.getName())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .status(entity.getStatus().name())
                .finalScore(entity.getFinalScore())
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
