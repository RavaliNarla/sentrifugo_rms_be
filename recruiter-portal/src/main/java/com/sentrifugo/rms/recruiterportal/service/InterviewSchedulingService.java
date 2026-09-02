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
            LocalTime current = window.getStartTime();
            int duration = window.getDurationMinutes();
            while (!current.plusMinutes(duration).isAfter(window.getEndTime())) {
                LocalTime end = current.plusMinutes(duration);
                slots.add(new Slot(window.getPanelId(), window.getInterviewDate(), current, end, duration));
                current = end;
            }
        }
        return slots;
    }

    private void sendInterviewEmail(CandidateEntity candidate, Slot slot) {
        try {
            InterviewPanelEntity panel = interviewPanelRepository.findById(slot.panelId()).orElse(null);
            String html = "<p>Dear " + candidate.getName() + ",</p>"
                    + "<p>Your interview has been scheduled.</p>"
                    + "<p><b>Date:</b> " + slot.date() + "<br/>"
                    + "<b>Time:</b> " + slot.start() + " - " + slot.end() + "<br/>"
                    + "<b>Panel:</b> " + (panel != null ? panel.getName() : "-") + "</p>"
                    + "<p>Please be available at the scheduled time.</p>";
            mailService.sendHtmlEmail(candidate.getEmail(), "Interview Scheduled", html);
        } catch (Exception e) {
            log.warn("Failed to send interview scheduling email: {}", e.getMessage());
        }
    }
}
