package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.common.util.SecurityUtils;
import com.sentrifugo.rms.db.entity.*;
import com.sentrifugo.rms.db.enums.CandidateStatus;
import com.sentrifugo.rms.db.repository.*;
import com.sentrifugo.rms.recruiterportal.dto.InterviewScheduleDTO;
import com.sentrifugo.rms.recruiterportal.dto.ScoreSubmitRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewPoolService {

    private static final BigDecimal PASS_MARK = BigDecimal.valueOf(50);

    private final CandidateRepository candidateRepository;
    private final InterviewScheduleRepository interviewScheduleRepository;
    private final InterviewPanelMemberRepository interviewPanelMemberRepository;
    private final PanelMemberScoreRepository panelMemberScoreRepository;
    private final InterviewPanelRepository interviewPanelRepository;
    private final SecurityUtils securityUtils;

    public Page<InterviewScheduleDTO> getInterviewPool(UUID positionId, List<CandidateStatus> statuses, String searchText, int page, int size) {
        List<CandidateStatus> effectiveStatuses = statuses != null && !statuses.isEmpty() ? statuses :
                List.of(CandidateStatus.SCHEDULED, CandidateStatus.QUALIFIED, CandidateStatus.DISQUALIFIED);
        Page<CandidateEntity> candidatesPage = candidateRepository.search(positionId, effectiveStatuses, searchText, PageRequest.of(page, size));
        return candidatesPage.map(this::toDto);
    }

    /** Candidates for the currently logged-in interviewer's panel(s), for a given position. */
    public List<InterviewScheduleDTO> getMyInterviews(UUID positionId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        List<UUID> myPanelIds = interviewPanelMemberRepository.findAll().stream()
                .filter(m -> m.getUserId().equals(currentUserId))
                .map(InterviewPanelMemberEntity::getPanelId)
                .toList();

        Page<CandidateEntity> candidatesPage = candidateRepository.search(positionId,
                List.of(CandidateStatus.SCHEDULED, CandidateStatus.QUALIFIED, CandidateStatus.DISQUALIFIED),
                "", PageRequest.of(0, 500));

        return candidatesPage.getContent().stream()
                .map(this::toDto)
                .filter(dto -> dto.getPanelId() != null && myPanelIds.contains(dto.getPanelId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void submitScore(ScoreSubmitRequest request) {
        UUID panelMemberId = securityUtils.getCurrentUserId();
        CandidateEntity candidate = candidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        InterviewScheduleEntity schedule = interviewScheduleRepository.findByCandidateId(candidate.getId())
                .orElseThrow(() -> new CommonException("Candidate has not been scheduled for an interview."));

        PanelMemberScoreEntity score = panelMemberScoreRepository
                .findByCandidateIdAndPanelMemberId(candidate.getId(), panelMemberId)
                .orElse(PanelMemberScoreEntity.builder()
                        .candidateId(candidate.getId())
                        .panelMemberId(panelMemberId)
                        .build());
        score.setScore(request.getScore());
        score.setComments(request.getComments());
        panelMemberScoreRepository.save(score);

        List<UUID> panelMemberIds = interviewPanelMemberRepository.findByPanelId(schedule.getPanelId()).stream()
                .map(InterviewPanelMemberEntity::getUserId)
                .toList();

        List<PanelMemberScoreEntity> allScores = panelMemberScoreRepository.findByCandidateId(candidate.getId());
        boolean allScored = panelMemberIds.stream().allMatch(id -> allScores.stream().anyMatch(s -> s.getPanelMemberId().equals(id)));

        if (allScored && !panelMemberIds.isEmpty()) {
            BigDecimal sum = allScores.stream()
                    .filter(s -> panelMemberIds.contains(s.getPanelMemberId()))
                    .map(PanelMemberScoreEntity::getScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal average = sum.divide(BigDecimal.valueOf(panelMemberIds.size()), 2, RoundingMode.HALF_UP);
            candidate.setFinalScore(average);
            candidate.setStatus(average.compareTo(PASS_MARK) >= 0 ? CandidateStatus.QUALIFIED : CandidateStatus.DISQUALIFIED);
            candidateRepository.save(candidate);
        }
    }

    private InterviewScheduleDTO toDto(CandidateEntity candidate) {
        InterviewScheduleEntity schedule = interviewScheduleRepository.findByCandidateId(candidate.getId()).orElse(null);
        String panelName = null;
        int membersTotal = 0;
        int membersScored = 0;
        UUID panelId = null;
        if (schedule != null) {
            panelId = schedule.getPanelId();
            panelName = interviewPanelRepository.findById(schedule.getPanelId()).map(InterviewPanelEntity::getName).orElse(null);
            membersTotal = interviewPanelMemberRepository.findByPanelId(schedule.getPanelId()).size();
            membersScored = panelMemberScoreRepository.findByCandidateId(candidate.getId()).size();
        }
        return InterviewScheduleDTO.builder()
                .candidateId(candidate.getId())
                .candidateName(candidate.getName())
                .applicationStatus(candidate.getStatus().name())
                .panelId(panelId)
                .panelName(panelName)
                .interviewDate(schedule != null ? schedule.getInterviewDate() : null)
                .startTime(schedule != null ? schedule.getStartTime() : null)
                .endTime(schedule != null ? schedule.getEndTime() : null)
                .durationMinutes(schedule != null ? schedule.getDurationMinutes() : null)
                .finalScore(candidate.getFinalScore())
                .membersScored(membersScored)
                .membersTotal(membersTotal)
                .build();
    }
}
