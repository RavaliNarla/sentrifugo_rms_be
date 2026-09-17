package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.service.MailService;
import com.sentrifugo.rms.db.entity.CandidateEntity;
import com.sentrifugo.rms.db.entity.InterviewPanelEntity;
import com.sentrifugo.rms.db.entity.InterviewScheduleEntity;
import com.sentrifugo.rms.db.enums.CandidateStatus;
import com.sentrifugo.rms.db.repository.CandidateRepository;
import com.sentrifugo.rms.db.repository.InterviewPanelRepository;
import com.sentrifugo.rms.db.repository.InterviewScheduleRepository;
import com.sentrifugo.rms.recruiterportal.dto.PanelWindowDTO;
import com.sentrifugo.rms.recruiterportal.dto.ScheduleInterviewRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Simplified version of the reference project's Add-Panel + Apply-to-All scheduling:
 * one location per job removes the need for zone/centre routing, so this just walks
 * each panel-day window in order, cuts it into fixed-duration slots, and assigns one
 * candidate per slot in application order.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSchedulingService {

    private final CandidateRepository candidateRepository;
    private final InterviewPanelRepository interviewPanelRepository;
    private final InterviewScheduleRepository interviewScheduleRepository;
    private final com.sentrifugo.rms.db.repository.JobPositionRepository jobPositionRepository;
    private final com.sentrifugo.rms.db.repository.LocationRepository locationRepository;
    private final MailService mailService;

    private record Slot(UUID panelId, java.time.LocalDate date, LocalTime start, LocalTime end, int duration) {
    }

    @Transactional
    public List<InterviewScheduleEntity> scheduleInterviews(ScheduleInterviewRequest request) {
        List<CandidateEntity> candidates = candidateRepository.findByIdIn(request.getCandidateIds()).stream()
                .sorted(Comparator.comparing(CandidateEntity::getCreatedDate))
                .toList();

        for (CandidateEntity candidate : candidates) {
            if (candidate.getStatus() != CandidateStatus.SHORTLISTED) {
                throw new CommonException("Candidate '" + candidate.getName() + "' must be SHORTLISTED before scheduling.");
            }
        }

        List<Slot> slots = generateSlots(request.getPanelWindows());

        if (slots.size() < candidates.size()) {
            throw new CommonException("Not enough interview slots (" + slots.size() + ") for the selected candidates (" + candidates.size() + "). Add more panel time or reduce candidates.");
        }

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
            saved.add(interviewScheduleRepository.save(schedule));

            candidate.setStatus(CandidateStatus.SCHEDULED);
            candidateRepository.save(candidate);

            sendInterviewEmail(candidate, slot);
        }
        return saved;
    }

    private List<Slot> generateSlots(List<PanelWindowDTO> windows) {
        List<Slot> slots = new ArrayList<>();
        for (PanelWindowDTO window : windows) {
            if (window.getDurationMinutes() == null || window.getDurationMinutes() <= 0) {
                throw new CommonException("Interview duration must be greater than 0 minutes.");
            }
            if (window.getStartTime() == null || window.getEndTime() == null) {
                throw new CommonException("Start time and end time are required for each panel window.");
            }
            if (!window.getStartTime().isBefore(window.getEndTime())) {
                throw new CommonException("Start time must be before end time for each panel window.");
            }

            // Use minute arithmetic (not LocalTime.plusMinutes in a while) so midnight wrap
            // or duration=0 can never create an infinite slot loop / OOM.
            int duration = window.getDurationMinutes();
            int startMins = window.getStartTime().toSecondOfDay() / 60;
            int endMins = window.getEndTime().toSecondOfDay() / 60;
            for (int cursor = startMins; cursor + duration <= endMins; cursor += duration) {
                LocalTime start = LocalTime.of(cursor / 60, cursor % 60);
                LocalTime end = LocalTime.of((cursor + duration) / 60, (cursor + duration) % 60);
                slots.add(new Slot(window.getPanelId(), window.getInterviewDate(), start, end, duration));
            }
        }
        return slots;
    }

    private void sendInterviewEmail(CandidateEntity candidate, Slot slot) {
        try {
            InterviewPanelEntity panel = interviewPanelRepository.findById(slot.panelId()).orElse(null);
            // Interview location is the position's plant location (Section 11 of the requirements doc).
            String location = jobPositionRepository.findById(candidate.getPositionId())
                    .map(p -> locationRepository.findById(p.getLocationId()).map(l -> l.getName()).orElse(null))
                    .orElse(null);
            String html = "<p>Dear " + candidate.getName() + ",</p>"
                    + "<p>Your interview has been scheduled.</p>"
                    + "<p><b>Date:</b> " + slot.date() + "<br/>"
                    + "<b>Time:</b> " + slot.start() + " - " + slot.end() + "<br/>"
                    + "<b>Interview Location:</b> " + (location != null ? location : "-") + "<br/>"
                    + "<b>Panel:</b> " + (panel != null ? panel.getName() : "-") + "</p>"
                    + "<p>Please be available at the scheduled time.</p>";
            mailService.sendHtmlEmail(candidate.getEmail(), "Interview Scheduled", html);
        } catch (Exception e) {
            log.warn("Failed to send interview scheduling email: {}", e.getMessage());
        }
    }
}
