package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum InterviewSchedulingStatus {

    PENDING,
    RESCHEDULED,
    SCHEDULED,
    SELECTED_FOR_NEXT_ROUND,
    NOT_AVAILABLE,
    SELECTED,
    REJECTED,
    CANCELLED,
    NOT_SCHEDULED,
    QUALIFIED,
    DISQUALIFIED,
    PROVISIONALLY_APPROVED,
    OFFER_AWAITED,
    ZONAL_REJECTED,
    ZONAL_ABSENT,
    INTERVIEW_ABSENT,
    COMPENSATION,
    RESCHEDULE_PENDING;

    @JsonCreator
    public static InterviewSchedulingStatus fromValue(String value) {
        if (value == null || value.isBlank()){
            throw new IllegalArgumentException("Invalid Scheduling Status.");
        }

        for (InterviewSchedulingStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        // Throwing IllegalArgumentException allows Spring to see the root cause clearly
        throw new IllegalArgumentException("Invalid Scheduling Status.");
    }
}
