package com.sentrifugo.rms.recruiterportal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * One-day schedule: panel + day + window.
 * Optional breaks are skipped when auto-packing slots.
 * Optional candidateSlots override auto-packing with explicit per-candidate times.
 */
public class ScheduleInterviewRequest {

    @NotEmpty(message = "Select at least one candidate")
    private List<UUID> candidateIds;

    @NotNull(message = "Select a panel")
    private UUID panelId;

    @NotNull(message = "Interview date is required")
    private LocalDate interviewDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    /** Defaults to 30 minutes when omitted (used for auto-packing and as fallback duration). */
    private Integer durationMinutes;

    /**
     * Interview round. Omit or 1 = first schedule (SHORTLISTED only).
     * For Schedule Next Round, must be current schedule round + 1 (QUALIFIED only).
     */
    private Integer round;

    /** Lunch / other gaps — interviews must not overlap these. */
    @Valid
    private List<BreakWindow> breaks;

    /**
     * Explicit per-candidate slots (matched by candidateId).
     * When present and covering all candidateIds, used instead of auto-packing.
     */
    @Valid
    private List<CandidateSlot> candidateSlots;

    public List<UUID> getCandidateIds() {
        return candidateIds;
    }

    public void setCandidateIds(List<UUID> candidateIds) {
        this.candidateIds = candidateIds;
    }

    public UUID getPanelId() {
        return panelId;
    }

    public void setPanelId(UUID panelId) {
        this.panelId = panelId;
    }

    public LocalDate getInterviewDate() {
        return interviewDate;
    }

    public void setInterviewDate(LocalDate interviewDate) {
        this.interviewDate = interviewDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
    }

    public List<BreakWindow> getBreaks() {
        return breaks;
    }

    public void setBreaks(List<BreakWindow> breaks) {
        this.breaks = breaks;
    }

    public List<CandidateSlot> getCandidateSlots() {
        return candidateSlots;
    }

    public void setCandidateSlots(List<CandidateSlot> candidateSlots) {
        this.candidateSlots = candidateSlots;
    }

    public static class BreakWindow {
        @NotNull(message = "Break start time is required")
        private LocalTime startTime;

        @NotNull(message = "Break end time is required")
        private LocalTime endTime;

        public LocalTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalTime startTime) {
            this.startTime = startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalTime endTime) {
            this.endTime = endTime;
        }
    }

    public static class CandidateSlot {
        @NotNull(message = "Candidate id is required for each slot")
        private UUID candidateId;

        @NotNull(message = "Slot start time is required")
        private LocalTime startTime;

        @NotNull(message = "Slot end time is required")
        private LocalTime endTime;

        public UUID getCandidateId() {
            return candidateId;
        }

        public void setCandidateId(UUID candidateId) {
            this.candidateId = candidateId;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalTime startTime) {
            this.startTime = startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalTime endTime) {
            this.endTime = endTime;
        }
    }
}
