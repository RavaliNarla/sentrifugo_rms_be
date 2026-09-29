package com.sentrifugo.rms.recruiterportal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Schedule Interviews page: one round spread across several days (each day = panel + date + window).
 * All days are saved in one transaction - either every day is scheduled or none are.
 * round / roundName apply to every day and override the per-day values.
 */
public class MultiDayScheduleRequest {

    /** Omit or 1 = first schedule; Schedule Next Round passes current round + 1. */
    private Integer round;

    /** Optional label shown in Interview Pool as "1 (Technical)". Max 120 chars. */
    private String roundName;

    @NotEmpty(message = "Add at least one interview day")
    @Valid
    private List<ScheduleInterviewRequest> days;

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
    }

    public String getRoundName() {
        return roundName;
    }

    public void setRoundName(String roundName) {
        this.roundName = roundName;
    }

    public List<ScheduleInterviewRequest> getDays() {
        return days;
    }

    public void setDays(List<ScheduleInterviewRequest> days) {
        this.days = days;
    }
}
