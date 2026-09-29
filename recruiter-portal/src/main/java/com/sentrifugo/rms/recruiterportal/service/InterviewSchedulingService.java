package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.common.service.MailService;
import com.sentrifugo.rms.common.service.NotificationService;
import com.sentrifugo.rms.common.util.IstTime;
import com.sentrifugo.rms.common.util.SecurityUtils;
import com.sentrifugo.rms.db.entity.*;
import com.sentrifugo.rms.db.enums.CandidateStatus;
import com.sentrifugo.rms.db.repository.*;
import com.sentrifugo.rms.recruiterportal.dto.InterviewScheduleListItemDTO;
import com.sentrifugo.rms.recruiterportal.dto.MultiDayScheduleRequest;
import com.sentrifugo.rms.recruiterportal.dto.ScheduleInterviewRequest;
import com.sentrifugo.rms.recruiterportal.email.RmsEmailTemplates;
import com.sentrifugo.rms.recruiterportal.util.IcsCalendarBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * One-day interview scheduling with invite Accept/Decline + ICS reminders.
 * Slot stays booked for INVITE_SENT / SCHEDULED; DECLINED frees the slot.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSchedulingService {

    private static final int DEFAULT_DURATION_MINUTES = 30;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    /** Statuses that keep a panel member's slot occupied. */
    private static final Set<CandidateStatus> SLOT_HOLDING_STATUSES = EnumSet.of(
            CandidateStatus.INVITE_SENT,
            CandidateStatus.SCHEDULED,
            CandidateStatus.QUALIFIED,
            CandidateStatus.DISQUALIFIED
    );

    private final CandidateRepository candidateRepository;
    private final InterviewPanelRepository interviewPanelRepository;
    private final InterviewPanelMemberRepository interviewPanelMemberRepository;
    private final InterviewScheduleRepository interviewScheduleRepository;
    private final JobPositionRepository jobPositionRepository;
    private final PositionTitleRepository positionTitleRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final NotificationService notificationService;
    private final InterviewRoundMetaRepository interviewRoundMetaRepository;
    private final PanelMemberScoreRepository panelMemberScoreRepository;
    private final SecurityUtils securityUtils;
    private final RmsEmailTemplates emailTemplates;

    @Value("${app.base.url}")
    private String appBaseUrl;

    private record Slot(UUID panelId, LocalDate date, LocalTime start, LocalTime end, int duration) {
    }

    /** Current round / slots of the candidates being rescheduled (read before anything is moved). */
    private record RescheduleContext(int round, String roundName, Set<UUID> oldPanelIds, Set<LocalDate> oldDates) {
    }

    @Transactional
    public List<InterviewScheduleEntity> scheduleInterviews(ScheduleInterviewRequest request) {
        return scheduleInterviewsInternal(request, false, Set.of());
    }

    /**
     * Schedule Interviews page: same round across several days. Each day reuses the single-day
     * logic; one transaction, so any failing day rolls back all days (invite emails are only
     * sent after commit).
     */
    @Transactional
    public List<InterviewScheduleEntity> scheduleInterviewsMultiDay(MultiDayScheduleRequest request) {
        assertValidDays(request.getDays());
        return saveDays(request.getDays(), request.getRound(), request.getRoundName(), false);
    }

    /**
     * Schedule Interviews page in reschedule mode: move INVITE_SENT / SCHEDULED interviews of one
     * round across one or more days. All days are validated up front and saved in one transaction.
     */
    @Transactional
    public List<InterviewScheduleEntity> rescheduleInterviewsMultiDay(MultiDayScheduleRequest request) {
        assertValidDays(request.getDays());
        List<UUID> allIds = request.getDays().stream()
                .flatMap(d -> d.getCandidateIds().stream())
                .toList();
        List<CandidateEntity> candidates = candidateRepository.findByIdIn(allIds);
        if (candidates.size() != allIds.size()) {
            throw new CommonException("One or more candidates were not found.");
        }
        RescheduleContext ctx = validateReschedule(candidates);
        String roundName = request.getRoundName() == null || request.getRoundName().isBlank()
                ? ctx.roundName() : request.getRoundName();
        List<InterviewScheduleEntity> saved = saveDays(request.getDays(), ctx.round(), roundName, true);
        refreshOldDayNotifications(ctx);
        return saved;
    }

    /** Each candidate on one day only; one panel used at most once per date. */
    private void assertValidDays(List<ScheduleInterviewRequest> days) {
        Set<UUID> seenCandidates = new HashSet<>();
        Set<String> seenPanelDates = new HashSet<>();
        for (int i = 0; i < days.size(); i++) {
            ScheduleInterviewRequest day = days.get(i);
            for (UUID candidateId : day.getCandidateIds()) {
                if (!seenCandidates.add(candidateId)) {
                    throw new CommonException("A candidate is assigned to more than one day. Assign each candidate to one day only.");
                }
            }
            if (!seenPanelDates.add(day.getPanelId() + "|" + day.getInterviewDate())) {
                throw new CommonException("Day " + (i + 1)
                        + ": the same panel is already used on this date in another day. Merge them into one day.");
            }
        }
    }

    private List<InterviewScheduleEntity> saveDays(List<ScheduleInterviewRequest> days, Integer round,
                                                   String roundName, boolean reschedule) {
        List<InterviewScheduleEntity> saved = new ArrayList<>();
        for (int i = 0; i < days.size(); i++) {
            ScheduleInterviewRequest day = days.get(i);
            day.setRound(round);
            day.setRoundName(roundName);
            // Candidates of this and later days still hold their old slots - those must not block
            // this day. Earlier days are already moved, so their new slots are checked normally.
            Set<UUID> notYetMoved = days.subList(i + 1, days.size()).stream()
                    .flatMap(d -> d.getCandidateIds().stream())
                    .collect(Collectors.toSet());
            try {
                saved.addAll(scheduleInterviewsInternal(day, reschedule, reschedule ? notYetMoved : Set.of()));
            } catch (CommonException e) {
                throw new CommonException("Day " + (i + 1) + " (" + day.getInterviewDate().format(DATE_FMT) + "): "
                        + e.getMessage());
            }
        }
        return saved;
    }

    /**
     * Reschedule existing INVITE_SENT / SCHEDULED interviews (same round). Blocked once any
     * panel member has scored that round.
     */
    @Transactional
    public List<InterviewScheduleEntity> rescheduleInterviews(ScheduleInterviewRequest request) {
        RescheduleContext ctx = validateReschedule(loadOrderedCandidates(request));
        request.setRound(ctx.round());
        if ((request.getRoundName() == null || request.getRoundName().isBlank()) && ctx.roundName() != null) {
            request.setRoundName(ctx.roundName());
        }
        List<InterviewScheduleEntity> saved = scheduleInterviewsInternal(request, true, Set.of());
        refreshOldDayNotifications(ctx);
        return saved;
    }

    /** Old panel/day interview counts changed - refresh those members' day notifications. */
    private void refreshOldDayNotifications(RescheduleContext ctx) {
        UUID schedulerId = securityUtils.getCurrentUserId();
        for (UUID panelId : ctx.oldPanelIds()) {
            for (LocalDate date : ctx.oldDates()) {
                List<InterviewPanelMemberEntity> members = interviewPanelMemberRepository.findByPanelId(panelId);
                for (InterviewPanelMemberEntity member : members) {
                    int dayCount = countActiveInterviewsForMember(member.getUserId(), date);
                    notificationService.upsertCommitteeInterviewDay(
                            member.getUserId(), date, dayCount, false, schedulerId);
                }
            }
        }
    }

    private RescheduleContext validateReschedule(List<CandidateEntity> candidates) {
        Integer sharedRound = null;
        String sharedRoundName = null;
        Set<UUID> oldPanelIds = new HashSet<>();
        Set<LocalDate> oldDates = new HashSet<>();
        for (CandidateEntity candidate : candidates) {
            if (candidate.getStatus() != CandidateStatus.INVITE_SENT
                    && candidate.getStatus() != CandidateStatus.SCHEDULED) {
                throw new CommonException("Candidate '" + candidate.getName()
                        + "' must be Invite Sent or Scheduled to reschedule.");
            }
            InterviewScheduleEntity existing = interviewScheduleRepository.findByCandidateId(candidate.getId())
                    .orElseThrow(() -> new CommonException("Candidate '" + candidate.getName()
                            + "' has no interview schedule to reschedule."));
            int round = existing.getRound() != null ? existing.getRound() : 1;
            if (sharedRound == null) {
                sharedRound = round;
                sharedRoundName = existing.getRoundName();
            } else if (!sharedRound.equals(round)) {
                throw new CommonException("Select candidates from the same interview round to reschedule together.");
            }
            assertNoScoresYet(candidate, round);
            oldPanelIds.add(existing.getPanelId());
            if (existing.getInterviewDate() != null) {
                oldDates.add(existing.getInterviewDate());
            }
        }
        return new RescheduleContext(sharedRound, sharedRoundName, oldPanelIds, oldDates);
    }

    /**
     * @param ignoreSlotsOf candidates (besides this day's) whose current slots must not count as
     *                      conflicts - used when a multi-day reschedule moves them on a later day.
     */
    private List<InterviewScheduleEntity> scheduleInterviewsInternal(ScheduleInterviewRequest request, boolean reschedule,
                                                                     Set<UUID> ignoreSlotsOf) {
        int duration = request.getDurationMinutes() != null ? request.getDurationMinutes() : DEFAULT_DURATION_MINUTES;
        if (duration <= 0) {
            throw new CommonException("Interview duration must be greater than 0 minutes.");
        }
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new CommonException("Start time must be before end time.");
        }
        if (!interviewPanelRepository.existsById(request.getPanelId())) {
            throw new ResourceNotFoundException("Panel not found");
        }

        List<TimeRangeMins> breaks = normalizeBreaks(request.getBreaks(), request.getStartTime(), request.getEndTime());

        List<CandidateEntity> candidates = loadOrderedCandidates(request);

        int targetRound = reschedule
                ? (request.getRound() != null ? request.getRound() : 1)
                : resolveAndValidateRound(request, candidates);
        String roundName = normalizeRoundName(request.getRoundName());

        List<Slot> slots = resolveSlots(request, candidates, duration, breaks);

        if (slots.size() < candidates.size()) {
            throw new CommonException("Not enough interview slots (" + slots.size()
                    + ") for " + candidates.size()
                    + " candidate(s). Extend the time window, shorten duration, remove/shorten breaks, or select fewer candidates.");
        }

        Set<UUID> candidateIds = candidates.stream().map(CandidateEntity::getId).collect(Collectors.toCollection(HashSet::new));
        candidateIds.addAll(ignoreSlotsOf);
        assertNoMemberConflicts(request.getPanelId(), request.getInterviewDate(),
                slots.subList(0, candidates.size()), candidateIds);
        assertNoInternalOverlaps(slots.subList(0, candidates.size()));

        List<InterviewScheduleEntity> saved = new ArrayList<>();
        List<PendingMail> mails = new ArrayList<>();
        InterviewPanelEntity panel = interviewPanelRepository.findById(request.getPanelId()).orElse(null);
        LocalDateTime now = IstTime.now();

        for (int i = 0; i < candidates.size(); i++) {
            CandidateEntity candidate = candidates.get(i);
            Slot slot = slots.get(i);

            // Reuse the candidate's row even if an earlier interview was cancelled (soft-deleted).
            InterviewScheduleEntity schedule = interviewScheduleRepository.findAnyByCandidateId(candidate.getId())
                    .orElse(InterviewScheduleEntity.builder().candidateId(candidate.getId()).build());
            schedule.setIsActive(true);
            archiveAcceptToken(schedule);
            UUID token = UUID.randomUUID();
            schedule.setPanelId(slot.panelId());
            schedule.setInterviewDate(slot.date());
            schedule.setStartTime(slot.start());
            schedule.setEndTime(slot.end());
            schedule.setDurationMinutes(slot.duration());
            schedule.setRound(targetRound);
            schedule.setRoundName(roundName);
            schedule.setAcceptToken(token);
            schedule.setInviteSentAt(now);
            schedule.setInviteRespondedAt(null);
            UUID schedulerId = securityUtils.getCurrentUserId();
            if (schedule.getCreatedBy() == null) {
                schedule.setCreatedBy(schedulerId);
            }
            schedule.setModifiedBy(schedulerId);
            saved.add(interviewScheduleRepository.save(schedule));
            upsertRoundMeta(candidate.getId(), targetRound, roundName, slot);

            candidate.setStatus(CandidateStatus.INVITE_SENT);
            candidate.setFinalScore(null);
            candidateRepository.save(candidate);

            String location = resolveLocationName(candidate);
            String positionTitle = resolvePositionTitle(candidate);
            PendingMail mail = buildCandidateInviteEmail(
                    candidate, slot, targetRound, roundName, panel, location, positionTitle, token);
            if (mail != null) {
                mails.add(mail);
            }
        }

        List<InterviewPanelMemberEntity> members =
                interviewPanelMemberRepository.findByPanelId(request.getPanelId());
        UUID schedulerId = securityUtils.getCurrentUserId();
        for (InterviewPanelMemberEntity member : members) {
            int dayCount = countActiveInterviewsForMember(member.getUserId(), request.getInterviewDate());
            notificationService.upsertCommitteeInterviewDay(
                    member.getUserId(), request.getInterviewDate(), dayCount, false, schedulerId);

            UserEntity user = userRepository.findById(member.getUserId()).orElse(null);
            if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
                List<IcsCalendarBuilder.Event> memberDayEvents =
                        buildActiveDayEventsForMember(member.getUserId(), request.getInterviewDate());
                PendingMail panelMail = buildPanelInviteEmail(user, request.getInterviewDate(), dayCount, memberDayEvents);
                if (panelMail != null) {
                    mails.add(panelMail);
                }
            }
        }

        queueMailsAfterCommit(mails);
        return saved;
    }

    /** Cancel INVITE_SENT / SCHEDULED interviews and free the panel slot. */
    @Transactional
    public int cancelInterviews(List<UUID> candidateIds) {
        if (candidateIds == null || candidateIds.isEmpty()) {
            throw new CommonException("Select at least one candidate to cancel.");
        }
        List<CandidateEntity> candidates = candidateRepository.findByIdIn(candidateIds);
        if (candidates.isEmpty()) {
            throw new CommonException("No candidates found for the given ids.");
        }
        Set<UUID> panelIds = new HashSet<>();
        Set<LocalDate> dates = new HashSet<>();
        int cancelled = 0;
        for (CandidateEntity candidate : candidates) {
            if (candidate.getStatus() != CandidateStatus.INVITE_SENT
                    && candidate.getStatus() != CandidateStatus.SCHEDULED) {
                throw new CommonException("Candidate '" + candidate.getName()
                        + "' is not Invite Sent / Scheduled — cannot cancel.");
            }
            InterviewScheduleEntity schedule = interviewScheduleRepository.findByCandidateId(candidate.getId())
                    .orElseThrow(() -> new CommonException("Candidate '" + candidate.getName()
                            + "' has no interview schedule."));
            int round = schedule.getRound() != null ? schedule.getRound() : 1;
            assertNoScoresYet(candidate, round);
            if (schedule.getPanelId() != null) {
                panelIds.add(schedule.getPanelId());
            }
            if (schedule.getInterviewDate() != null) {
                dates.add(schedule.getInterviewDate());
            }
            archiveAcceptToken(schedule);
            schedule.setAcceptToken(null);
            interviewScheduleRepository.delete(schedule);

            // Round 1 → Shortlisted; later rounds → Qualified (ready to re-schedule that round).
            candidate.setStatus(round <= 1 ? CandidateStatus.SHORTLISTED : CandidateStatus.QUALIFIED);
            candidate.setFinalScore(null);
            candidateRepository.save(candidate);
            cancelled++;
        }

        UUID actorId = securityUtils.getCurrentUserId();
        for (UUID panelId : panelIds) {
            for (LocalDate date : dates) {
                List<InterviewPanelMemberEntity> members = interviewPanelMemberRepository.findByPanelId(panelId);
                for (InterviewPanelMemberEntity member : members) {
                    int dayCount = countActiveInterviewsForMember(member.getUserId(), date);
                    notificationService.upsertCommitteeInterviewDay(
                            member.getUserId(), date, dayCount, false, actorId);
                }
            }
        }
        return cancelled;
    }

    private List<CandidateEntity> loadOrderedCandidates(ScheduleInterviewRequest request) {
        List<CandidateEntity> candidates = candidateRepository.findByIdIn(request.getCandidateIds()).stream()
                .sorted(Comparator.comparing(CandidateEntity::getCreatedDate))
                .toList();
        if (candidates.isEmpty()) {
            throw new CommonException("No candidates found for the given ids.");
        }
        if (request.getCandidateSlots() != null && !request.getCandidateSlots().isEmpty()) {
            Map<UUID, CandidateEntity> byId = candidates.stream()
                    .collect(Collectors.toMap(CandidateEntity::getId, c -> c, (a, b) -> a));
            List<CandidateEntity> ordered = new ArrayList<>();
            for (UUID id : request.getCandidateIds()) {
                CandidateEntity c = byId.get(id);
                if (c == null) {
                    throw new CommonException("Candidate not found: " + id);
                }
                ordered.add(c);
            }
            return ordered;
        }
        return candidates;
    }

    private void assertNoScoresYet(CandidateEntity candidate, int round) {
        List<PanelMemberScoreEntity> scores = panelMemberScoreRepository
                .findByCandidateIdAndRound(candidate.getId(), round);
        if (scores != null && !scores.isEmpty()) {
            throw new CommonException("Candidate '" + candidate.getName()
                    + "' already has interviewer score(s) for this round and cannot be rescheduled or cancelled.");
        }
    }

    /**
     * Candidate clicked Accept or Decline in the invite email.
     * @return ACCEPTED | DECLINED | SUPERSEDED | INVALID | ALREADY_ACCEPTED | ALREADY_DECLINED
     */
    @Transactional
    public String decideInvite(UUID token, boolean accept) {
        if (token == null) {
            return "INVALID";
        }
        Optional<InterviewScheduleEntity> found = interviewScheduleRepository.findByAcceptToken(token);
        if (found.isEmpty()) {
            if (isSupersededToken(token)) {
                return "SUPERSEDED";
            }
            return "INVALID";
        }

        InterviewScheduleEntity schedule = found.get();
        CandidateEntity candidate = candidateRepository.findById(schedule.getCandidateId()).orElse(null);
        if (candidate == null) {
            return "INVALID";
        }

        if (candidate.getStatus() == CandidateStatus.SCHEDULED) {
            return "ALREADY_ACCEPTED";
        }
        if (candidate.getStatus() == CandidateStatus.DECLINED) {
            return "ALREADY_DECLINED";
        }
        if (candidate.getStatus() != CandidateStatus.INVITE_SENT) {
            return "SUPERSEDED";
        }

        // Link valid through the interview date inclusive (expires the day after).
        LocalDate interviewDate = schedule.getInterviewDate();
        if (interviewDate != null && com.sentrifugo.rms.common.util.IstTime.today().isAfter(interviewDate)) {
            return "EXPIRED";
        }
        UUID panelId = schedule.getPanelId();

        if (accept) {
            candidate.setStatus(CandidateStatus.SCHEDULED);
            schedule.setInviteRespondedAt(IstTime.now());
            candidateRepository.save(candidate);
            interviewScheduleRepository.save(schedule);
            notifyRecruiterOfInviteDecision(schedule, candidate, true);
            return "ACCEPTED";
        }

        candidate.setStatus(CandidateStatus.DECLINED);
        schedule.setInviteRespondedAt(IstTime.now());
        candidateRepository.save(candidate);
        interviewScheduleRepository.save(schedule);
        notifyRecruiterOfInviteDecision(schedule, candidate, false);

        // Free the slot: refresh panel member day counts (DECLINED no longer counts).
        List<InterviewPanelMemberEntity> members = interviewPanelMemberRepository.findByPanelId(panelId);
        for (InterviewPanelMemberEntity member : members) {
            int dayCount = countActiveInterviewsForMember(member.getUserId(), interviewDate);
            notificationService.upsertCommitteeInterviewDay(member.getUserId(), interviewDate, dayCount, false);
        }
        return "DECLINED";
    }

    private boolean isSupersededToken(UUID token) {
        return interviewScheduleRepository.existsBySupersededTokensContaining(token.toString());
    }

    private void archiveAcceptToken(InterviewScheduleEntity schedule) {
        UUID previous = schedule.getAcceptToken();
        if (previous == null) {
            return;
        }
        String existing = schedule.getSupersededTokens();
        if (existing == null || existing.isBlank()) {
            schedule.setSupersededTokens(previous.toString());
        } else if (!existing.contains(previous.toString())) {
            schedule.setSupersededTokens(existing + "," + previous);
        }
    }

    private int countActiveInterviewsForMember(UUID userId, LocalDate date) {
        List<UUID> memberPanelIds = interviewPanelMemberRepository.findByUserId(userId).stream()
                .map(InterviewPanelMemberEntity::getPanelId)
                .distinct()
                .toList();
        if (memberPanelIds.isEmpty()) {
            return 0;
        }
        List<InterviewScheduleEntity> schedules =
                interviewScheduleRepository.findByPanelIdInAndInterviewDate(memberPanelIds, date);
        if (schedules.isEmpty()) {
            return 0;
        }
        Set<UUID> candidateIds = schedules.stream().map(InterviewScheduleEntity::getCandidateId).collect(Collectors.toSet());
        Map<UUID, CandidateStatus> statusById = candidateRepository.findByIdIn(new ArrayList<>(candidateIds)).stream()
                .collect(Collectors.toMap(CandidateEntity::getId, CandidateEntity::getStatus, (a, b) -> a));
        return (int) schedules.stream()
                .filter(s -> SLOT_HOLDING_STATUSES.contains(statusById.get(s.getCandidateId())))
                .count();
    }

    private List<IcsCalendarBuilder.Event> buildActiveDayEventsForMember(UUID userId, LocalDate date) {
        List<UUID> memberPanelIds = interviewPanelMemberRepository.findByUserId(userId).stream()
                .map(InterviewPanelMemberEntity::getPanelId)
                .distinct()
                .toList();
        if (memberPanelIds.isEmpty()) {
            return List.of();
        }
        List<InterviewScheduleEntity> schedules =
                interviewScheduleRepository.findByPanelIdInAndInterviewDate(memberPanelIds, date);
        if (schedules.isEmpty()) {
            return List.of();
        }
        Set<UUID> candidateIds = schedules.stream().map(InterviewScheduleEntity::getCandidateId).collect(Collectors.toSet());
        Map<UUID, CandidateEntity> candidates = candidateRepository.findByIdIn(new ArrayList<>(candidateIds)).stream()
                .collect(Collectors.toMap(CandidateEntity::getId, c -> c, (a, b) -> a));

        List<IcsCalendarBuilder.Event> events = new ArrayList<>();
        for (InterviewScheduleEntity schedule : schedules) {
            CandidateEntity candidate = candidates.get(schedule.getCandidateId());
            if (candidate == null || !SLOT_HOLDING_STATUSES.contains(candidate.getStatus())) {
                continue;
            }
            String positionTitle = resolvePositionTitle(candidate);
            String location = resolveLocationName(candidate);
            events.add(new IcsCalendarBuilder.Event(
                    schedule.getId() + "@interview.sentrifugo-rms",
                    "Interview: " + candidate.getName() + (positionTitle != null ? " — " + positionTitle : ""),
                    "Candidate: " + candidate.getName()
                            + "\\nRound: " + (schedule.getRound() != null ? schedule.getRound() : 1)
                            + "\\nTime: " + schedule.getStartTime() + " – " + schedule.getEndTime()
                            + (candidate.getEmail() != null ? "\\nEmail: " + candidate.getEmail() : "")
                            + "\\nStatus: " + candidate.getStatus().name(),
                    location,
                    schedule.getInterviewDate(),
                    schedule.getStartTime(),
                    schedule.getEndTime()
            ));
        }
        events.sort(Comparator
                .comparing(IcsCalendarBuilder.Event::start, Comparator.nullsLast(Comparator.naturalOrder())));
        return events;
    }

    private static String normalizeRoundName(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 120) {
            throw new CommonException("Round name must be at most 120 characters.");
        }
        return trimmed;
    }

    private void upsertRoundMeta(UUID candidateId, int round, String roundName, Slot slot) {
        InterviewRoundMetaEntity meta = interviewRoundMetaRepository
                .findByCandidateIdAndRound(candidateId, round)
                .orElse(InterviewRoundMetaEntity.builder()
                        .candidateId(candidateId)
                        .round(round)
                        .build());
        meta.setRoundName(roundName);
        meta.setPanelId(slot.panelId());
        meta.setInterviewDate(slot.date());
        meta.setStartTime(slot.start());
        meta.setEndTime(slot.end());
        meta.setDurationMinutes(slot.duration());
        interviewRoundMetaRepository.save(meta);
    }

    /**
     * Round 1 (or omitted): SHORTLISTED, or re-invite DECLINED / refresh INVITE_SENT.
     * Round R+1: all QUALIFIED, same current schedule round R, request.round == R+1.
     */
    private int resolveAndValidateRound(ScheduleInterviewRequest request, List<CandidateEntity> candidates) {
        int requested = request.getRound() != null ? request.getRound() : 1;
        if (requested < 1) {
            throw new CommonException("Interview round must be at least 1.");
        }

        if (requested == 1) {
            for (CandidateEntity candidate : candidates) {
                CandidateStatus status = candidate.getStatus();
                if (status != CandidateStatus.SHORTLISTED
                        && status != CandidateStatus.DECLINED
                        && status != CandidateStatus.INVITE_SENT) {
                    throw new CommonException("Candidate '" + candidate.getName()
                            + "' must be Shortlisted (or Declined / Invite Sent to re-invite) before scheduling Round 1.");
                }
            }
            return 1;
        }

        Integer sharedCurrentRound = null;
        for (CandidateEntity candidate : candidates) {
            if (candidate.getStatus() != CandidateStatus.QUALIFIED) {
                throw new CommonException("Candidate '" + candidate.getName()
                        + "' must be Qualified to schedule a further interview round.");
            }
            InterviewScheduleEntity existing = interviewScheduleRepository.findByCandidateId(candidate.getId())
                    .orElseThrow(() -> new CommonException("Candidate '" + candidate.getName()
                            + "' has no prior interview schedule."));
            int current = existing.getRound() != null ? existing.getRound() : 1;
            if (sharedCurrentRound == null) {
                sharedCurrentRound = current;
            } else if (!sharedCurrentRound.equals(current)) {
                throw new CommonException(
                        "Select candidates from the same interview round to schedule the next round together.");
            }
            if (requested != current + 1) {
                throw new CommonException("Next round for '" + candidate.getName()
                        + "' must be Round " + (current + 1) + " (currently Round " + current + ").");
            }
        }
        return requested;
    }

    public Page<InterviewScheduleListItemDTO> listSchedules(
            String view, UUID id, LocalDate from, LocalDate to, int page, int size) {
        if (id == null) {
            throw new CommonException("Select a panel or interviewer.");
        }
        if (from == null || to == null) {
            throw new CommonException("From and To dates are required.");
        }
        if (to.isBefore(from)) {
            throw new CommonException("To date cannot be before From date.");
        }

        Page<InterviewRoundMetaEntity> metas;
        String viewKey = view == null ? "PANEL" : view.trim().toUpperCase(Locale.ROOT);
        if ("INTERVIEWER".equals(viewKey)) {
            List<UUID> panelIds = interviewPanelMemberRepository.findByUserId(id).stream()
                    .map(InterviewPanelMemberEntity::getPanelId)
                    .distinct()
                    .toList();
            if (panelIds.isEmpty()) {
                return Page.empty(PageRequest.of(page, size));
            }
            metas = interviewRoundMetaRepository.findByPanelIdsAndDateRange(panelIds, from, to, PageRequest.of(page, size));
        } else if ("PANEL".equals(viewKey)) {
            metas = interviewRoundMetaRepository.findByPanelAndDateRange(id, from, to, PageRequest.of(page, size));
        } else {
            throw new CommonException("View must be PANEL or INTERVIEWER.");
        }

        return metas.map(this::toListItemFromMeta);
    }

    private void assertNoMemberConflicts(UUID panelId, LocalDate date, List<Slot> newSlots,
                                         Set<UUID> ignoreCandidateIds) {
        List<UUID> memberIds = interviewPanelMemberRepository.findByPanelId(panelId).stream()
                .map(InterviewPanelMemberEntity::getUserId)
                .toList();
        if (memberIds.isEmpty()) {
            throw new CommonException("Selected panel has no members. Add members before scheduling.");
        }

        Set<UUID> relatedPanelIds = interviewPanelMemberRepository.findByUserIdIn(memberIds).stream()
                .map(InterviewPanelMemberEntity::getPanelId)
                .collect(Collectors.toCollection(HashSet::new));

        List<InterviewScheduleEntity> existingRaw = interviewScheduleRepository
                .findByPanelIdInAndInterviewDate(relatedPanelIds, date).stream()
                .filter(s -> ignoreCandidateIds == null || !ignoreCandidateIds.contains(s.getCandidateId()))
                .toList();

        Set<UUID> existingCandidateIds = existingRaw.stream()
                .map(InterviewScheduleEntity::getCandidateId)
                .collect(Collectors.toSet());
        Map<UUID, CandidateStatus> statusById = existingCandidateIds.isEmpty() ? Map.of()
                : candidateRepository.findByIdIn(new ArrayList<>(existingCandidateIds)).stream()
                .collect(Collectors.toMap(CandidateEntity::getId, CandidateEntity::getStatus, (a, b) -> a));

        // DECLINED (and any non-holding status) does not block the slot.
        List<InterviewScheduleEntity> existing = existingRaw.stream()
                .filter(s -> SLOT_HOLDING_STATUSES.contains(statusById.get(s.getCandidateId())))
                .toList();

        Map<UUID, String> userNames = userRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getName, (a, b) -> a));
        Map<UUID, List<UUID>> panelMembers = interviewPanelMemberRepository.findByPanelIdIn(new ArrayList<>(relatedPanelIds))
                .stream()
                .collect(Collectors.groupingBy(
                        InterviewPanelMemberEntity::getPanelId,
                        Collectors.mapping(InterviewPanelMemberEntity::getUserId, Collectors.toList())));

        for (Slot slot : newSlots) {
            for (InterviewScheduleEntity existingSchedule : existing) {
                if (!timesOverlap(slot.start(), slot.end(), existingSchedule.getStartTime(), existingSchedule.getEndTime())) {
                    continue;
                }
                List<UUID> busyMembers = panelMembers.getOrDefault(existingSchedule.getPanelId(), List.of()).stream()
                        .filter(memberIds::contains)
                        .toList();
                if (busyMembers.isEmpty()) {
                    continue;
                }
                String names = busyMembers.stream()
                        .map(uid -> userNames.getOrDefault(uid, "Panel member"))
                        .collect(Collectors.joining(", "));
                throw new CommonException("Scheduling conflict: " + names + " already has an interview on "
                        + date + " from " + existingSchedule.getStartTime() + " to " + existingSchedule.getEndTime()
                        + ". Pick another panel, day, or time window.");
            }
        }
    }

    private static boolean timesOverlap(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private record TimeRangeMins(int start, int end) {
    }

    private List<TimeRangeMins> normalizeBreaks(List<ScheduleInterviewRequest.BreakWindow> raw,
                                                LocalTime windowStart, LocalTime windowEnd) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        int winStart = toMinutes(windowStart);
        int winEnd = toMinutes(windowEnd);
        List<TimeRangeMins> breaks = new ArrayList<>();
        for (ScheduleInterviewRequest.BreakWindow b : raw) {
            if (b.getStartTime() == null || b.getEndTime() == null) {
                throw new CommonException("Each break needs a start and end time.");
            }
            if (!b.getStartTime().isBefore(b.getEndTime())) {
                throw new CommonException("Break end time must be after break start time.");
            }
            int s = toMinutes(b.getStartTime());
            int e = toMinutes(b.getEndTime());
            if (s < winStart || e > winEnd) {
                throw new CommonException("Break " + b.getStartTime() + "–" + b.getEndTime()
                        + " must fall inside the interview window "
                        + windowStart + "–" + windowEnd + ".");
            }
            for (TimeRangeMins existing : breaks) {
                if (s < existing.end() && existing.start() < e) {
                    throw new CommonException("Breaks overlap each other. Adjust break times.");
                }
            }
            breaks.add(new TimeRangeMins(s, e));
        }
        breaks.sort(Comparator.comparingInt(TimeRangeMins::start));
        return breaks;
    }

    private List<Slot> resolveSlots(ScheduleInterviewRequest request, List<CandidateEntity> candidates,
                                    int duration, List<TimeRangeMins> breaks) {
        List<ScheduleInterviewRequest.CandidateSlot> explicit = request.getCandidateSlots();
        if (explicit != null && !explicit.isEmpty()) {
            return resolveExplicitSlots(request, candidates, duration, breaks, explicit);
        }
        return generateSlots(request.getPanelId(), request.getInterviewDate(),
                request.getStartTime(), request.getEndTime(), duration, breaks);
    }

    private List<Slot> resolveExplicitSlots(ScheduleInterviewRequest request,
                                            List<CandidateEntity> candidates,
                                            int duration,
                                            List<TimeRangeMins> breaks,
                                            List<ScheduleInterviewRequest.CandidateSlot> explicit) {
        Map<UUID, ScheduleInterviewRequest.CandidateSlot> byCandidate = new LinkedHashMap<>();
        for (ScheduleInterviewRequest.CandidateSlot cs : explicit) {
            if (cs.getCandidateId() == null || cs.getStartTime() == null || cs.getEndTime() == null) {
                throw new CommonException("Each candidate slot needs candidateId, startTime, and endTime.");
            }
            if (!cs.getStartTime().isBefore(cs.getEndTime())) {
                throw new CommonException("Slot end must be after start for candidate " + cs.getCandidateId() + ".");
            }
            if (byCandidate.put(cs.getCandidateId(), cs) != null) {
                throw new CommonException("Duplicate slot for the same candidate.");
            }
        }

        int winStart = toMinutes(request.getStartTime());
        int winEnd = toMinutes(request.getEndTime());
        LocalDate date = request.getInterviewDate();

        List<Slot> slots = new ArrayList<>();
        for (CandidateEntity candidate : candidates) {
            ScheduleInterviewRequest.CandidateSlot cs = byCandidate.get(candidate.getId());
            if (cs == null) {
                throw new CommonException("Missing time slot for candidate '" + candidate.getName() + "'.");
            }
            int s = toMinutes(cs.getStartTime());
            int e = toMinutes(cs.getEndTime());
            if (s < winStart || e > winEnd) {
                throw new CommonException("Slot for '" + candidate.getName() + "' (" + cs.getStartTime()
                        + "–" + cs.getEndTime() + ") is outside the interview window ("
                        + request.getStartTime() + "–" + request.getEndTime() + ").");
            }
            for (TimeRangeMins br : breaks) {
                if (s < br.end() && br.start() < e) {
                    throw new CommonException("Slot for '" + candidate.getName() + "' overlaps a break ("
                            + fromMinutes(br.start()) + "–" + fromMinutes(br.end()) + ").");
                }
            }
            int slotDuration = e - s;
            if (slotDuration <= 0) {
                throw new CommonException("Invalid slot duration for '" + candidate.getName() + "'.");
            }
            int useDuration = slotDuration == duration ? duration : slotDuration;
            slots.add(new Slot(request.getPanelId(), date, cs.getStartTime(), cs.getEndTime(), useDuration));
        }
        return slots;
    }

    private void assertNoInternalOverlaps(List<Slot> slots) {
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                Slot a = slots.get(i);
                Slot b = slots.get(j);
                if (!a.date().equals(b.date())) {
                    continue;
                }
                if (timesOverlap(a.start(), a.end(), b.start(), b.end())) {
                    throw new CommonException("Interview slots overlap on " + a.date() + " ("
                            + a.start() + "–" + a.end() + " and " + b.start() + "–" + b.end()
                            + "). Adjust candidate times so they do not overlap.");
                }
            }
        }
    }

    private static int toMinutes(LocalTime t) {
        return t.toSecondOfDay() / 60;
    }

    private static LocalTime fromMinutes(int mins) {
        return LocalTime.of(mins / 60, mins % 60);
    }

    private List<Slot> generateSlots(UUID panelId, LocalDate date, LocalTime startTime, LocalTime endTime,
                                     int duration, List<TimeRangeMins> breaks) {
        List<Slot> slots = new ArrayList<>();
        int startMins = toMinutes(startTime);
        int endMins = toMinutes(endTime);
        int cursor = startMins;
        int guard = 0;
        while (cursor + duration <= endMins && guard < 5000) {
            guard++;
            int slotEnd = cursor + duration;
            TimeRangeMins hit = null;
            for (TimeRangeMins br : breaks) {
                if (cursor < br.end() && br.start() < slotEnd) {
                    hit = br;
                    break;
                }
            }
            if (hit != null) {
                cursor = Math.max(cursor + 1, hit.end());
                continue;
            }
            slots.add(new Slot(panelId, date, fromMinutes(cursor), fromMinutes(slotEnd), duration));
            cursor = slotEnd;
        }
        return slots;
    }

    private InterviewScheduleListItemDTO toListItemFromMeta(InterviewRoundMetaEntity meta) {
        CandidateEntity candidate = candidateRepository.findById(meta.getCandidateId()).orElse(null);
        InterviewPanelEntity panel = meta.getPanelId() != null
                ? interviewPanelRepository.findById(meta.getPanelId()).orElse(null)
                : null;
        List<InterviewPanelMemberEntity> members = meta.getPanelId() != null
                ? interviewPanelMemberRepository.findByPanelId(meta.getPanelId())
                : List.of();
        Map<UUID, String> userNames = userRepository.findAllById(
                members.stream().map(InterviewPanelMemberEntity::getUserId).toList()
        ).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getName, (a, b) -> a));

        String positionTitle = null;
        String locationName = null;
        if (candidate != null) {
            JobPositionEntity position = jobPositionRepository.findById(candidate.getPositionId()).orElse(null);
            if (position != null) {
                positionTitle = positionTitleRepository.findById(position.getPositionTitleId())
                        .map(PositionTitleEntity::getName).orElse(null);
                locationName = locationRepository.findById(position.getLocationId())
                        .map(LocationEntity::getName).orElse(null);
            }
        }

        String roundName = meta.getRoundName();
        Integer round = meta.getRound() != null ? meta.getRound() : 1;

        return InterviewScheduleListItemDTO.builder()
                .id(meta.getId())
                .interviewDate(meta.getInterviewDate())
                .startTime(meta.getStartTime())
                .endTime(meta.getEndTime())
                .durationMinutes(meta.getDurationMinutes())
                .candidateName(candidate != null ? candidate.getName() : null)
                .candidateEmail(candidate != null ? candidate.getEmail() : null)
                .candidateStatus(candidate != null ? candidate.getStatus().name() : null)
                .panelName(panel != null ? panel.getName() : null)
                .panelMemberNames(members.stream().map(m -> userNames.get(m.getUserId())).filter(Objects::nonNull).toList())
                .positionTitleName(positionTitle)
                .locationName(locationName)
                .round(round)
                .roundName(roundName)
                .build();
    }

    private InterviewScheduleListItemDTO toListItem(InterviewScheduleEntity schedule) {
        CandidateEntity candidate = candidateRepository.findById(schedule.getCandidateId()).orElse(null);
        InterviewPanelEntity panel = interviewPanelRepository.findById(schedule.getPanelId()).orElse(null);
        List<InterviewPanelMemberEntity> members = interviewPanelMemberRepository.findByPanelId(schedule.getPanelId());
        Map<UUID, String> userNames = userRepository.findAllById(
                members.stream().map(InterviewPanelMemberEntity::getUserId).toList()
        ).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getName, (a, b) -> a));

        String positionTitle = null;
        String locationName = null;
        if (candidate != null) {
            JobPositionEntity position = jobPositionRepository.findById(candidate.getPositionId()).orElse(null);
            if (position != null) {
                positionTitle = positionTitleRepository.findById(position.getPositionTitleId())
                        .map(PositionTitleEntity::getName).orElse(null);
                locationName = locationRepository.findById(position.getLocationId())
                        .map(LocationEntity::getName).orElse(null);
            }
        }

        return InterviewScheduleListItemDTO.builder()
                .id(schedule.getId())
                .interviewDate(schedule.getInterviewDate())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .durationMinutes(schedule.getDurationMinutes())
                .candidateName(candidate != null ? candidate.getName() : null)
                .candidateEmail(candidate != null ? candidate.getEmail() : null)
                .candidateStatus(candidate != null ? candidate.getStatus().name() : null)
                .panelName(panel != null ? panel.getName() : null)
                .panelMemberNames(members.stream().map(m -> userNames.get(m.getUserId())).filter(Objects::nonNull).toList())
                .positionTitleName(positionTitle)
                .locationName(locationName)
                .round(schedule.getRound() != null ? schedule.getRound() : 1)
                .roundName(schedule.getRoundName())
                .build();
    }

    private String resolveLocationName(CandidateEntity candidate) {
        return jobPositionRepository.findById(candidate.getPositionId())
                .map(p -> locationRepository.findById(p.getLocationId()).map(LocationEntity::getName).orElse(null))
                .orElse(null);
    }

    private String resolvePositionTitle(CandidateEntity candidate) {
        return jobPositionRepository.findById(candidate.getPositionId())
                .map(p -> positionTitleRepository.findById(p.getPositionTitleId()).map(PositionTitleEntity::getName).orElse(null))
                .orElse(null);
    }

    private PendingMail buildCandidateInviteEmail(CandidateEntity candidate, Slot slot, int round, String roundName,
                                                  InterviewPanelEntity panel, String location,
                                                  String positionTitle, UUID token) {
        if (candidate.getEmail() == null || candidate.getEmail().isBlank()) {
            return null;
        }
        String acceptUrl = appBaseUrl + "/api/v1/public/interviews/" + token + "/accept";
        String declineUrl = appBaseUrl + "/api/v1/public/interviews/" + token + "/decline";
        String resolvedRoundName = roundName;
        if (resolvedRoundName == null || resolvedRoundName.isBlank()) {
            InterviewRoundMetaEntity meta = interviewRoundMetaRepository
                    .findByCandidateIdAndRound(candidate.getId(), round).orElse(null);
            if (meta != null && meta.getRoundName() != null && !meta.getRoundName().isBlank()) {
                resolvedRoundName = meta.getRoundName();
            }
        }
        RmsEmailTemplates.BuiltEmail email = emailTemplates.interviewInvite(
                candidate.getName(), positionTitle, round, resolvedRoundName,
                slot.date(), slot.start(), slot.end(), location,
                panel != null ? panel.getName() : null, acceptUrl, declineUrl);
        IcsCalendarBuilder.Event event = new IcsCalendarBuilder.Event(
                token + "@invite.sentrifugo-rms",
                "Interview" + (positionTitle != null ? " — " + positionTitle : "")
                        + " (Round " + round
                        + (resolvedRoundName != null && !resolvedRoundName.isBlank() ? " – " + resolvedRoundName : "")
                        + ")",
                "Please use Accept / Decline in your invite email to confirm. This calendar entry is a reminder only.",
                location, slot.date(), slot.start(), slot.end());
        byte[] ics = IcsCalendarBuilder.singleEventIcs(event);
        List<MailService.Attachment> attachments = List.of(
                new MailService.Attachment("interview-invite.ics", ics, IcsCalendarBuilder.CONTENT_TYPE));
        return new PendingMail(candidate.getEmail(), email.subject(), email.html(), attachments);
    }

    private void notifyRecruiterOfInviteDecision(InterviewScheduleEntity schedule, CandidateEntity candidate, boolean accepted) {
        try {
            LinkedHashSet<UUID> ownerIds = new LinkedHashSet<>();
            if (schedule.getCreatedBy() != null) {
                ownerIds.add(schedule.getCreatedBy());
            }
            if (schedule.getModifiedBy() != null) {
                ownerIds.add(schedule.getModifiedBy());
            }
            if (ownerIds.isEmpty()) {
                log.warn("No recruiter owner on interview schedule {} — skipping invite-decision email", schedule.getId());
                return;
            }
            String positionTitle = resolvePositionTitle(candidate);
            int round = schedule.getRound() != null ? schedule.getRound() : 1;
            Set<String> sent = new HashSet<>();
            for (UUID ownerId : ownerIds) {
                UserEntity recruiter = userRepository.findById(ownerId).orElse(null);
                if (recruiter == null || recruiter.getEmail() == null || recruiter.getEmail().isBlank()) {
                    continue;
                }
                if (!sent.add(recruiter.getEmail().trim().toLowerCase())) {
                    continue;
                }
                RmsEmailTemplates.BuiltEmail email = emailTemplates.recruiterInviteResponse(
                        recruiter.getName(), candidate.getName(), positionTitle, round, schedule.getRoundName(), accepted);
                emailTemplates.sendAsync(recruiter.getEmail(), email);
            }
        } catch (Exception e) {
            log.warn("Failed to notify recruiter of invite decision: {}", e.getMessage());
        }
    }

    private PendingMail buildPanelInviteEmail(UserEntity user, LocalDate interviewDate, int dayCount,
                                              List<IcsCalendarBuilder.Event> dayEvents) {
        if (user.getEmail() == null || user.getEmail().isBlank() || dayEvents == null || dayEvents.isEmpty()) {
            return null;
        }
        String day = IstTime.fmtDate(interviewDate);
        RmsEmailTemplates.BuiltEmail email = emailTemplates.panelDaySchedule(user.getName(), day, dayCount);
        List<IcsCalendarBuilder.NamedIcs> files = IcsCalendarBuilder.multiEventIcsFiles(
                "interviews-" + interviewDate, dayEvents);
        List<MailService.Attachment> attachments = files.stream()
                .map(f -> new MailService.Attachment(f.fileName(), f.bytes(), IcsCalendarBuilder.CONTENT_TYPE))
                .toList();
        return new PendingMail(user.getEmail(), email.subject(), email.html(), attachments);
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void queueMailsAfterCommit(List<PendingMail> mails) {
        if (mails == null || mails.isEmpty()) {
            return;
        }
        Runnable send = () -> {
            for (PendingMail mail : mails) {
                try {
                    mailService.sendHtmlEmailAsync(mail.to(), mail.subject(), mail.html(), mail.attachments());
                } catch (Exception e) {
                    log.warn("Failed to queue interview email to {}: {}", mail.to(), e.getMessage());
                }
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

    private record PendingMail(String to, String subject, String html, List<MailService.Attachment> attachments) {
    }
}
