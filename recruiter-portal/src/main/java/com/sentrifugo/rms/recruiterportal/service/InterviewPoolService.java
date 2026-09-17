package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.common.util.SecurityUtils;
import com.sentrifugo.rms.db.entity.*;
import com.sentrifugo.rms.db.enums.CandidateStatus;
import com.sentrifugo.rms.db.repository.*;
import com.sentrifugo.rms.recruiterportal.dto.InterviewScheduleDTO;
import com.sentrifugo.rms.recruiterportal.dto.PanelMemberScoreViewDTO;
import com.sentrifugo.rms.recruiterportal.dto.ScoreSubmitRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewPoolService {

    /** Rating scale 1-10 (decimals allowed), pass mark 5. Decision dropdown is advisory only. */
    private static final BigDecimal PASS_MARK = BigDecimal.valueOf(5);
    private static final BigDecimal SCORE_MIN = BigDecimal.ONE;
    private static final BigDecimal SCORE_MAX = BigDecimal.TEN;

    private final CandidateRepository candidateRepository;
    private final InterviewScheduleRepository interviewScheduleRepository;
    private final InterviewPanelMemberRepository interviewPanelMemberRepository;
    private final PanelMemberScoreRepository panelMemberScoreRepository;
    private final InterviewPanelRepository interviewPanelRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public Page<InterviewScheduleDTO> getInterviewPool(UUID positionId, List<CandidateStatus> statuses, String searchText, int page, int size) {
        List<CandidateStatus> effectiveStatuses = statuses != null && !statuses.isEmpty() ? statuses :
                List.of(CandidateStatus.SCHEDULED, CandidateStatus.QUALIFIED, CandidateStatus.DISQUALIFIED);
        Page<CandidateEntity> candidatesPage = candidateRepository.search(positionId, effectiveStatuses, searchText, PageRequest.of(page, size));
        return candidatesPage.map(c -> toDto(c, null, true));
    }

    /** Candidates for the currently logged-in interviewer's panel(s), for a given position, optionally filtered to one interview date. */
    public List<InterviewScheduleDTO> getMyInterviews(UUID positionId, LocalDate interviewDate) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        List<UUID> myPanelIds = interviewPanelMemberRepository.findAll().stream()
                .filter(m -> m.getUserId().equals(currentUserId))
                .map(InterviewPanelMemberEntity::getPanelId)
                .toList();

        Page<CandidateEntity> candidatesPage = candidateRepository.search(positionId,
                List.of(CandidateStatus.SCHEDULED, CandidateStatus.QUALIFIED, CandidateStatus.DISQUALIFIED),
                "", PageRequest.of(0, 500));

        return candidatesPage.getContent().stream()
                .map(c -> toDto(c, currentUserId, false))
                .filter(dto -> dto.getPanelId() != null && myPanelIds.contains(dto.getPanelId()))
                .filter(dto -> interviewDate == null || interviewDate.equals(dto.getInterviewDate()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void submitScore(ScoreSubmitRequest request) {
        UUID panelMemberId = securityUtils.getCurrentUserId();
        CandidateEntity candidate = candidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        InterviewScheduleEntity schedule = interviewScheduleRepository.findByCandidateId(candidate.getId())
                .orElseThrow(() -> new CommonException("Candidate has not been scheduled for an interview."));

        int round = schedule.getRound() != null ? schedule.getRound() : 1;

        BigDecimal scoreValue = request.getScore().setScale(2, RoundingMode.HALF_UP);
        if (scoreValue.compareTo(SCORE_MIN) < 0 || scoreValue.compareTo(SCORE_MAX) > 0) {
            throw new CommonException("Score must be between 1 and 10 (decimals allowed, e.g. 7.5).");
        }

        PanelMemberScoreEntity score = panelMemberScoreRepository
                .findByCandidateIdAndPanelMemberIdAndRound(candidate.getId(), panelMemberId, round)
                .orElse(PanelMemberScoreEntity.builder()
                        .candidateId(candidate.getId())
                        .panelMemberId(panelMemberId)
                        .round(round)
                        .build());
        score.setScore(scoreValue);
        score.setRationale(request.getRationale());
        // Stored for recruiter visibility; does NOT drive QUALIFIED / DISQUALIFIED.
        score.setDecision(request.getDecision());
        panelMemberScoreRepository.save(score);

        List<UUID> panelMemberIds = interviewPanelMemberRepository.findByPanelId(schedule.getPanelId()).stream()
                .map(InterviewPanelMemberEntity::getUserId)
                .toList();

        List<PanelMemberScoreEntity> roundScores = panelMemberScoreRepository
                .findByCandidateIdAndRound(candidate.getId(), round);
        boolean allScored = panelMemberIds.stream()
                .allMatch(id -> roundScores.stream().anyMatch(s -> s.getPanelMemberId().equals(id)));

        if (allScored && !panelMemberIds.isEmpty()) {
            BigDecimal sum = roundScores.stream()
                    .filter(s -> panelMemberIds.contains(s.getPanelMemberId()))
                    .map(PanelMemberScoreEntity::getScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal average = sum.divide(BigDecimal.valueOf(panelMemberIds.size()), 2, RoundingMode.HALF_UP);
            candidate.setFinalScore(average);
            // Qualification is by average score only (pass mark 5). Per-interviewer Decision is informational.
            candidate.setStatus(average.compareTo(PASS_MARK) >= 0 ? CandidateStatus.QUALIFIED : CandidateStatus.DISQUALIFIED);
            candidateRepository.save(candidate);
        }
    }

    private InterviewScheduleDTO toDto(CandidateEntity candidate, UUID currentUserId, boolean includeMemberScores) {
        InterviewScheduleEntity schedule = interviewScheduleRepository.findByCandidateId(candidate.getId()).orElse(null);
        String panelName = null;
        int membersTotal = 0;
        int membersScored = 0;
        UUID panelId = null;
        Integer round = null;
        BigDecimal myScore = null;
        String myRationale = null;
        String myDecision = null;
        List<PanelMemberScoreViewDTO> memberScores = null;
        if (schedule != null) {
            panelId = schedule.getPanelId();
            round = schedule.getRound() != null ? schedule.getRound() : 1;
            panelName = interviewPanelRepository.findById(schedule.getPanelId()).map(InterviewPanelEntity::getName).orElse(null);
            List<UUID> panelMemberIds = interviewPanelMemberRepository.findByPanelId(schedule.getPanelId()).stream()
                    .map(InterviewPanelMemberEntity::getUserId)
                    .toList();
            membersTotal = panelMemberIds.size();
            List<PanelMemberScoreEntity> roundScores = panelMemberScoreRepository
                    .findByCandidateIdAndRound(candidate.getId(), round).stream()
                    .filter(s -> panelMemberIds.contains(s.getPanelMemberId()))
                    .toList();
            membersScored = roundScores.size();
            if (currentUserId != null) {
                PanelMemberScoreEntity mine = roundScores.stream()
                        .filter(s -> s.getPanelMemberId().equals(currentUserId))
                        .findFirst().orElse(null);
                if (mine != null) {
                    myScore = mine.getScore();
                    myRationale = mine.getRationale();
                    myDecision = mine.getDecision();
                }
            }
            if (includeMemberScores && !roundScores.isEmpty()) {
                Map<UUID, String> names = userRepository.findAllById(
                        roundScores.stream().map(PanelMemberScoreEntity::getPanelMemberId).toList()
                ).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getName, (a, b) -> a));
                memberScores = new ArrayList<>();
                for (PanelMemberScoreEntity s : roundScores) {
                    memberScores.add(PanelMemberScoreViewDTO.builder()
                            .interviewerName(names.getOrDefault(s.getPanelMemberId(), "Interviewer"))
                            .score(s.getScore())
                            .rationale(s.getRationale())
                            .decision(s.getDecision())
                            .build());
                }
            }
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
                .round(round)
                .myScore(myScore)
                .myRationale(myRationale)
                .myDecision(myDecision)
                .memberScores(memberScores)
                .build();
    }
}
