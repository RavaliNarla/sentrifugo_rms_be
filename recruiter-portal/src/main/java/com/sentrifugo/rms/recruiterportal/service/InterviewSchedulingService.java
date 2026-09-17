package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.common.service.MailService;
import com.sentrifugo.rms.db.entity.*;
import com.sentrifugo.rms.db.enums.CandidateStatus;
import com.sentrifugo.rms.db.repository.*;
import com.sentrifugo.rms.recruiterportal.dto.InterviewScheduleListItemDTO;
import com.sentrifugo.rms.recruiterportal.dto.ScheduleInterviewRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple one-day interview scheduling: pick any panel, a date, and a time window.
 * Slots are carved by duration; panel members must not already be busy in overlapping slots.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSchedulingService {

    private static final int DEFAULT_DURATION_MINUTES = 30;

    private final CandidateRepository candidateRepository;
    private final InterviewPanelRepository interviewPanelRepository;
    private final InterviewPanelMemberRepository interviewPanelMemberRepository;
    private final InterviewScheduleRepository interviewScheduleRepository;
    private final JobPositionRepository jobPositionRepository;
    private final PositionTitleRepository positionTitleRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    private record Slot(UUID panelId, LocalDate date, LocalTime start, LocalTime end, int duration) {
    }

    @Transactional
    public List<InterviewScheduleEntity> scheduleInterviews(ScheduleInterviewRequest request) {
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

        List<CandidateEntity> candidates = candidateRepository.findByIdIn(request.getCandidateIds()).stream()
                .sorted(Comparator.comparing(CandidateEntity::getCreatedDate))
                .toList();
        if (candidates.isEmpty()) {
            throw new CommonException("No candidates found for the given ids.");
        }

        int targetRound = resolveAndValidateRound(request, candidates);

        List<Slot> slots = generateSlots(
                request.getPanelId(), request.getInterviewDate(),
                request.getStartTime(), request.getEndTime(), duration);

        if (slots.size() < candidates.size()) {
            throw new CommonException("Not enough interview slots (" + slots.size()
                    + ") for " + candidates.size() + " candidate(s). Extend the time window, shorten duration, or select fewer candidates.");
        }

        Set<UUID> candidateIds = candidates.stream().map(CandidateEntity::getId).collect(Collectors.toSet());
        assertNoMemberConflicts(request.getPanelId(), request.getInterviewDate(),
                slots.subList(0, candidates.size()), candidateIds);

        List<InterviewScheduleEntity> saved = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            CandidateEntity candidate = candidates.get(i);
            Slot slot = slots.get(i);

            InterviewScheduleEntity schedule = interviewScheduleRepository.findByCandidateId(candidate.getId())
                    .orElse(InterviewScheduleEntity.builder().candidateId(candidate.getId()).build());
            schedule.setPanelId(slot.panelId());
            schedule.setInterviewDate(slot.date());
            schedule.setStartTime(slot.start());
            schedule.setEndTime(slot.end());
            schedule.setDurationMinutes(slot.duration());
            schedule.setRound(targetRound);
            saved.add(interviewScheduleRepository.save(schedule));

            candidate.setStatus(CandidateStatus.SCHEDULED);
            candidate.setFinalScore(null);
            candidateRepository.save(candidate);

            sendInterviewEmail(candidate, slot, targetRound);
        }
        return saved;
    }

    /**
     * Round 1 (or omitted): all candidates SHORTLISTED.
     * Round R+1: all QUALIFIED, same current schedule round R, request.round == R+1.
     */
    private int resolveAndValidateRound(ScheduleInterviewRequest request, List<CandidateEntity> candidates) {
        int requested = request.getRound() != null ? request.getRound() : 1;
        if (requested < 1) {
            throw new CommonException("Interview round must be at least 1.");
        }

        if (requested == 1) {
            for (CandidateEntity candidate : candidates) {
                if (candidate.getStatus() != CandidateStatus.SHORTLISTED) {
                    throw new CommonException("Candidate '" + candidate.getName()
                            + "' must be Shortlisted before scheduling Round 1.");
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

    /**
     * Read-only browse: schedules for a panel, or for all panels that include a given interviewer.
     */
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

        Page<InterviewScheduleEntity> schedules;
        String viewKey = view == null ? "PANEL" : view.trim().toUpperCase(Locale.ROOT);
        if ("INTERVIEWER".equals(viewKey)) {
            List<UUID> panelIds = interviewPanelMemberRepository.findByUserId(id).stream()
                    .map(InterviewPanelMemberEntity::getPanelId)
                    .distinct()
                    .toList();
            if (panelIds.isEmpty()) {
                return Page.empty(PageRequest.of(page, size));
            }
            schedules = interviewScheduleRepository.findByPanelIdsAndDateRange(panelIds, from, to, PageRequest.of(page, size));
        } else if ("PANEL".equals(viewKey)) {
            schedules = interviewScheduleRepository.findByPanelAndDateRange(id, from, to, PageRequest.of(page, size));
        } else {
            throw new CommonException("View must be PANEL or INTERVIEWER.");
        }

        return schedules.map(this::toListItem);
    }

    private void assertNoMemberConflicts(UUID panelId, LocalDate date, List<Slot> newSlots,
                                         Set<UUID> ignoreCandidateIds) {
        List<UUID> memberIds = interviewPanelMemberRepository.findByPanelId(panelId).stream()
                .map(InterviewPanelMemberEntity::getUserId)
                .toList();
        if (memberIds.isEmpty()) {
            throw new CommonException("Selected panel has no members. Add members before scheduling.");
        }

        // Any panel that shares at least one member with the chosen panel.
        Set<UUID> relatedPanelIds = interviewPanelMemberRepository.findByUserIdIn(memberIds).stream()
                .map(InterviewPanelMemberEntity::getPanelId)
                .collect(Collectors.toCollection(HashSet::new));

        List<InterviewScheduleEntity> existing = interviewScheduleRepository
                .findByPanelIdInAndInterviewDate(relatedPanelIds, date).stream()
                // Allow overwriting / next-round for the candidates being scheduled.
                .filter(s -> ignoreCandidateIds == null || !ignoreCandidateIds.contains(s.getCandidateId()))
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

    private List<Slot> generateSlots(UUID panelId, LocalDate date, LocalTime startTime, LocalTime endTime, int duration) {
        List<Slot> slots = new ArrayList<>();
        int startMins = startTime.toSecondOfDay() / 60;
        int endMins = endTime.toSecondOfDay() / 60;
        for (int cursor = startMins; cursor + duration <= endMins; cursor += duration) {
            LocalTime start = LocalTime.of(cursor / 60, cursor % 60);
            LocalTime end = LocalTime.of((cursor + duration) / 60, (cursor + duration) % 60);
            slots.add(new Slot(panelId, date, start, end, duration));
        }
        return slots;
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
                .build();
    }

    private void sendInterviewEmail(CandidateEntity candidate, Slot slot, int round) {
        try {
            InterviewPanelEntity panel = interviewPanelRepository.findById(slot.panelId()).orElse(null);
            String location = jobPositionRepository.findById(candidate.getPositionId())
                    .map(p -> locationRepository.findById(p.getLocationId()).map(LocationEntity::getName).orElse(null))
                    .orElse(null);
            String html = "<p>Dear " + candidate.getName() + ",</p>"
                    + "<p>Your interview has been scheduled.</p>"
                    + "<p><b>Round:</b> " + round + "<br/>"
                    + "<b>Date:</b> " + slot.date() + "<br/>"
                    + "<b>Time:</b> " + slot.start() + " - " + slot.end() + "<br/>"
                    + "<b>Interview Location:</b> " + (location != null ? location : "-") + "<br/>"
                    + "<b>Panel:</b> " + (panel != null ? panel.getName() : "-") + "</p>"
                    + "<p>Please be available at the scheduled time.</p>";
            mailService.sendHtmlEmail(candidate.getEmail(), "Interview Scheduled - Round " + round, html);
        } catch (Exception e) {
            log.warn("Failed to send interview scheduling email: {}", e.getMessage());
        }
    }
}
