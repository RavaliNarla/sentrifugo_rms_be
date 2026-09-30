package com.sentrifugo.rms.recruiterportal.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.common.util.SecurityUtils;
import com.sentrifugo.rms.db.entity.*;
import com.sentrifugo.rms.db.enums.CandidateStatus;
import com.sentrifugo.rms.db.repository.*;
import com.sentrifugo.rms.recruiterportal.dto.InterviewRoundFeedbackDTO;
import com.sentrifugo.rms.recruiterportal.dto.InterviewScheduleDTO;
import com.sentrifugo.rms.recruiterportal.dto.JobRequisitionDTO;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.sentrifugo.rms.recruiterportal.email.RmsEmailTemplates;

@Service
@RequiredArgsConstructor
public class InterviewPoolService {

    /** Rating scale 1-10 (decimals allowed), pass mark 5. Decision dropdown is advisory only. */
    private static final BigDecimal PASS_MARK = BigDecimal.valueOf(5);
    private static final BigDecimal SCORE_MIN = BigDecimal.ONE;
    private static final BigDecimal SCORE_MAX = BigDecimal.TEN;
    private static final List<String> DECISIONS = List.of(
            "STRONG_HIRE", "HIRE", "HOLD", "DO_NOT_HIRE",
            // legacy values still accepted if already saved
            "SELECT", "REJECT");
    private static final List<String> COMPETENCY_KEYS = List.of(
            "TECHNICAL_KNOWLEDGE", "RELEVANT_EXPERIENCE", "COMMUNICATION",
            "PROBLEM_SOLVING", "ATTITUDE_APPROACH");

    private final CandidateRepository candidateRepository;
    private final InterviewScheduleRepository interviewScheduleRepository;
    private final InterviewPanelMemberRepository interviewPanelMemberRepository;
    private final PanelMemberScoreRepository panelMemberScoreRepository;
    private final InterviewPanelRepository interviewPanelRepository;
    private final JobPositionRepository jobPositionRepository;
    private final JobRequisitionRepository jobRequisitionRepository;
    private final PositionTitleRepository positionTitleRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final InterviewRoundMetaRepository interviewRoundMetaRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;
    private final RmsEmailTemplates emailTemplates;

    private static final List<CandidateStatus> INTERVIEW_POOL_STATUSES =
            List.of(CandidateStatus.INVITE_SENT, CandidateStatus.SCHEDULED, CandidateStatus.DECLINED,
                    CandidateStatus.QUALIFIED, CandidateStatus.DISQUALIFIED);

    /** round = optional round filter (R1, R2, ...); null = all rounds. */
    public Page<InterviewScheduleDTO> getInterviewPool(UUID positionId, List<CandidateStatus> statuses, Integer round,
                                                       String searchText, int page, int size) {
        List<CandidateStatus> effectiveStatuses = statuses != null && !statuses.isEmpty() ? statuses : INTERVIEW_POOL_STATUSES;
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<CandidateEntity> candidatesPage = round == null
                ? candidateRepository.search(positionId, effectiveStatuses, searchText, pageRequest)
                : candidateRepository.searchByRound(positionId, effectiveStatuses, searchText, round, pageRequest);
        return candidatesPage.map(c -> toDto(c, null, true));
    }

    /** Levels (rounds) present in this position's Interview Pool, ascending - always includes 1. */
    public List<Integer> getInterviewPoolRounds(UUID positionId) {
        java.util.TreeSet<Integer> rounds = new java.util.TreeSet<>();
        interviewScheduleRepository.findRoundsForPosition(positionId, INTERVIEW_POOL_STATUSES).stream()
                .filter(java.util.Objects::nonNull)
                .forEach(rounds::add);
        rounds.add(1);
        return List.copyOf(rounds);
    }

    /** Candidates who accepted (or are already in scoring) for the logged-in interviewer's panel(s). */
    // INVITE_SENT and DECLINED are hidden from the interviewer list (and so from its dropdowns).
    private static final List<CandidateStatus> INTERVIEWER_VISIBLE_STATUSES =
            List.of(CandidateStatus.SCHEDULED, CandidateStatus.QUALIFIED, CandidateStatus.DISQUALIFIED);

    private List<UUID> currentUserPanelIds() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        return interviewPanelMemberRepository.findAll().stream()
                .filter(m -> m.getUserId().equals(currentUserId))
                .map(InterviewPanelMemberEntity::getPanelId)
                .toList();
    }

    /** Positions where the current user's panel(s) have interviews visible in the interviewer list. */
    private List<JobPositionEntity> myInterviewPositions() {
        List<UUID> myPanelIds = currentUserPanelIds();
        if (myPanelIds.isEmpty()) {
            return List.of();
        }
        List<UUID> positionIds = interviewScheduleRepository.findPositionIdsForPanels(myPanelIds, INTERVIEWER_VISIBLE_STATUSES);
        return positionIds.isEmpty() ? List.of() : jobPositionRepository.findAllById(positionIds);
    }

    /** Interviewer Schedule "Requisition" dropdown: only requisitions with interviews on my panels. */
    public List<JobRequisitionDTO> getMyInterviewRequisitions() {
        Set<UUID> requisitionIds = myInterviewPositions().stream()
                .map(JobPositionEntity::getRequisitionId)
                .collect(Collectors.toSet());
        if (requisitionIds.isEmpty()) {
            return List.of();
        }
        return jobRequisitionRepository.findAllById(requisitionIds).stream()
                .sorted(Comparator.comparing(JobRequisitionEntity::getRequisitionCode,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(r -> JobRequisitionDTO.builder()
                        .id(r.getId())
                        .title(r.getTitle())
                        .requisitionCode(r.getRequisitionCode())
                        .status(r.getStatus().name())
                        .build())
                .toList();
    }

    /** Interviewer Schedule "Position" dropdown: position ids under a requisition with interviews on my panels. */
    public List<UUID> getMyInterviewPositionIds(UUID requisitionId) {
        return myInterviewPositions().stream()
                .filter(p -> requisitionId.equals(p.getRequisitionId()))
                .map(JobPositionEntity::getId)
                .toList();
    }

    public List<InterviewScheduleDTO> getMyInterviews(UUID positionId, LocalDate interviewDate) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        List<UUID> myPanelIds = currentUserPanelIds();

        Page<CandidateEntity> candidatesPage = candidateRepository.search(positionId,
                INTERVIEWER_VISIBLE_STATUSES, "", PageRequest.of(0, 500));

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

        boolean hasScore = request.getScore() != null;
        String rationale = request.getRationale() != null ? request.getRationale().trim() : "";
        String decision = request.getDecision() != null ? request.getDecision().trim().toUpperCase() : "";
        boolean hasRationale = !rationale.isEmpty();
        boolean hasDecision = !decision.isEmpty();
        Map<String, Integer> competency = normalizeCompetency(request.getCompetencyRatings());
        boolean competencyStarted = competency.values().stream().anyMatch(v -> v != null);
        String keyObservations = request.getKeyObservations() != null ? request.getKeyObservations().trim() : "";

        if (competencyStarted) {
            for (String key : COMPETENCY_KEYS) {
                Integer v = competency.get(key);
                if (v == null) {
                    throw new CommonException("If any competency rating is filled, all five competencies must be rated (1–5).");
                }
                if (v < 1 || v > 5) {
                    throw new CommonException("Competency ratings must be between 1 and 5.");
                }
            }
            // Filling competency requires the main three fields as well.
            if (!hasScore || !hasRationale || !hasDecision) {
                throw new CommonException("Competency assessment requires Rating, Rationale, and Decision to be filled for this candidate.");
            }
        }

        boolean anyMain = hasScore || hasRationale || hasDecision;
        if (!anyMain && !competencyStarted && keyObservations.isEmpty()) {
            throw new CommonException("Nothing to save for this candidate. Fill Rating, Rationale, and Decision together.");
        }
        if (anyMain && !(hasScore && hasRationale && hasDecision)) {
            throw new CommonException("For each candidate, Rating, Rationale, and Decision must all be filled together (or all left blank).");
        }
        if (!hasScore) {
            throw new CommonException("Rating is required when saving a score.");
        }

        BigDecimal scoreValue = request.getScore().setScale(2, RoundingMode.HALF_UP);
        if (scoreValue.compareTo(SCORE_MIN) < 0 || scoreValue.compareTo(SCORE_MAX) > 0) {
            throw new CommonException("Score must be between 1 and 10 (decimals allowed, e.g. 7.5).");
        }
        if (!DECISIONS.contains(decision)) {
            throw new CommonException("Decision must be Strong Hire, Hire, Hold, or Do Not Hire.");
        }

        PanelMemberScoreEntity score = panelMemberScoreRepository
                .findByCandidateIdAndPanelMemberIdAndRound(candidate.getId(), panelMemberId, round)
                .orElse(PanelMemberScoreEntity.builder()
                        .candidateId(candidate.getId())
                        .panelMemberId(panelMemberId)
                        .round(round)
                        .build());
        score.setScore(scoreValue);
        score.setRationale(rationale);
        // Stored for recruiter visibility; does NOT drive QUALIFIED / DISQUALIFIED.
        score.setDecision(decision);
        try {
            score.setCompetencyJson(competencyStarted ? objectMapper.writeValueAsString(competency) : null);
        } catch (Exception e) {
            throw new CommonException("Could not save competency assessment.");
        }
        score.setKeyObservations(keyObservations.isEmpty() ? null : keyObservations);
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
            boolean qualified = average.compareTo(PASS_MARK) >= 0;
            candidate.setStatus(qualified ? CandidateStatus.QUALIFIED : CandidateStatus.DISQUALIFIED);
            candidateRepository.save(candidate);
            try {
                if (candidate.getEmail() != null && !candidate.getEmail().isBlank()) {
                    JobPositionEntity position = jobPositionRepository.findById(candidate.getPositionId()).orElse(null);
                    String positionTitle = position != null
                            ? positionTitleRepository.findById(position.getPositionTitleId()).map(PositionTitleEntity::getName).orElse(null)
                            : null;
                    String locationName = position != null
                            ? locationRepository.findById(position.getLocationId()).map(LocationEntity::getName).orElse(null)
                            : null;
                    RmsEmailTemplates.BuiltEmail email = emailTemplates.interviewOutcome(
                            candidate.getName(), positionTitle, locationName, round, schedule.getRoundName(),
                            qualified, schedule.getInterviewDate());
                    emailTemplates.sendAsync(candidate.getEmail(), email, true, "interview-result.pdf");
                }
            } catch (Exception e) {
                // best-effort email
            }
        }
    }

    private Map<String, Integer> normalizeCompetency(Map<String, Integer> raw) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (String key : COMPETENCY_KEYS) {
            out.put(key, null);
        }
        if (raw == null) {
            return out;
        }
        for (String key : COMPETENCY_KEYS) {
            Integer v = raw.get(key);
            if (v == null) {
                // also accept camelCase from older clients
                continue;
            }
            out.put(key, v);
        }
        return out;
    }

    private Map<String, Integer> parseCompetencyJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private InterviewScheduleDTO toDto(CandidateEntity candidate, UUID currentUserId, boolean includeMemberScores) {
        InterviewScheduleEntity schedule = interviewScheduleRepository.findByCandidateId(candidate.getId()).orElse(null);
        String panelName = null;
        int membersTotal = 0;
        int membersScored = 0;
        UUID panelId = null;
        Integer round = null;
        String roundName = null;
        BigDecimal myScore = null;
        String myRationale = null;
        String myDecision = null;
        Map<String, Integer> myCompetencyRatings = null;
        String myKeyObservations = null;
        List<PanelMemberScoreViewDTO> memberScores = null;
        List<InterviewRoundFeedbackDTO> roundFeedback = null;
        if (schedule != null) {
            panelId = schedule.getPanelId();
            round = schedule.getRound() != null ? schedule.getRound() : 1;
            roundName = schedule.getRoundName();
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
                    myCompetencyRatings = parseCompetencyJson(mine.getCompetencyJson());
                    myKeyObservations = mine.getKeyObservations();
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
                            .competencyRatings(parseCompetencyJson(s.getCompetencyJson()))
                            .keyObservations(s.getKeyObservations())
                            .build());
                }
            }
            if (includeMemberScores) {
                roundFeedback = buildRoundFeedback(candidate.getId(), schedule);
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
                .roundName(roundName)
                .myScore(myScore)
                .myRationale(myRationale)
                .myDecision(myDecision)
                .myCompetencyRatings(myCompetencyRatings)
                .myKeyObservations(myKeyObservations)
                .memberScores(memberScores)
                .roundFeedback(roundFeedback)
                .build();
    }

    /**
     * All scored rounds for this candidate, newest first. Round names come from interview_round_meta
     * (survives schedule overwrite); current schedule roundName is a fallback for the live round.
     */
    private List<InterviewRoundFeedbackDTO> buildRoundFeedback(UUID candidateId, InterviewScheduleEntity schedule) {
        List<PanelMemberScoreEntity> allScores = panelMemberScoreRepository.findByCandidateId(candidateId);
        if (allScores.isEmpty()) {
            return List.of();
        }

        Map<Integer, String> roundNames = interviewRoundMetaRepository.findByCandidateId(candidateId).stream()
                .filter(m -> m.getRound() != null)
                .collect(Collectors.toMap(
                        InterviewRoundMetaEntity::getRound,
                        m -> m.getRoundName() != null ? m.getRoundName() : "",
                        (a, b) -> a.isBlank() ? b : a));
        if (schedule.getRound() != null && schedule.getRoundName() != null && !schedule.getRoundName().isBlank()) {
            roundNames.putIfAbsent(schedule.getRound(), schedule.getRoundName());
        }

        Set<UUID> interviewerIds = allScores.stream()
                .map(PanelMemberScoreEntity::getPanelMemberId)
                .collect(Collectors.toSet());
        Map<UUID, String> names = userRepository.findAllById(interviewerIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getName, (a, b) -> a));

        Map<Integer, List<PanelMemberScoreEntity>> byRound = allScores.stream()
                .collect(Collectors.groupingBy(s -> s.getRound() != null ? s.getRound() : 1));

        return byRound.entrySet().stream()
                .sorted(Map.Entry.<Integer, List<PanelMemberScoreEntity>>comparingByKey().reversed())
                .map(entry -> {
                    Integer r = entry.getKey();
                    String name = roundNames.get(r);
                    if (name != null && name.isBlank()) {
                        name = null;
                    }
                    List<PanelMemberScoreViewDTO> scores = entry.getValue().stream()
                            .sorted(Comparator.comparing(
                                    s -> names.getOrDefault(s.getPanelMemberId(), "Interviewer"),
                                    String.CASE_INSENSITIVE_ORDER))
                            .map(s -> PanelMemberScoreViewDTO.builder()
                                    .interviewerName(names.getOrDefault(s.getPanelMemberId(), "Interviewer"))
                                    .score(s.getScore())
                                    .rationale(s.getRationale())
                                    .decision(s.getDecision())
                                    .competencyRatings(parseCompetencyJson(s.getCompetencyJson()))
                                    .keyObservations(s.getKeyObservations())
                                    .build())
                            .toList();
                    return InterviewRoundFeedbackDTO.builder()
                            .round(r)
                            .roundName(name)
                            .scores(scores)
                            .build();
                })
                .toList();
    }
}
