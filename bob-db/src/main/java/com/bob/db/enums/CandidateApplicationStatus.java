package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CandidateApplicationStatus {
    // TODO: need to chech with harsha
    PENDING,
    APPLIED,
    RESCHEDULED,
    NOT_SCHEDULED,
    SCHEDULED,
    SELECTED_FOR_NEXT_ROUND,
    NOT_AVAILABLE,
    SELECTED,
    REJECTED,
    DISQUALIFIED,
    CANCELLED,
    SHORTLISTED,
    ELIGIBLE,
    OFFERED,
    OFFER_REJECTED,
    OFFER_ACCEPTED,
    DISCREPANCY,
    PROVISIONALLY_APPROVED,
    OFFER_AWAITED,
    OFFER_SENT,
    ZONAL_REJECTED,
    ZONAL_ABSENT,
    INTERVIEW_ABSENT,
    COMPENSATION_PENDING,
    COMPENSATION_APPROVED,
    COMPENSATION_REJECTED,
    COMPENSATION_RENEGOTITATE,
    SCHEDULE_PENDING,
    RESCHEDULE_PENDING,
    PRE_ONBOARDING_PENDING,
    PRE_ONBOARDING_COMPLETED,
    ONBOARDED;

    @JsonCreator
    public static CandidateApplicationStatus fromValue(String value) {
        if (value == null || value.isBlank()){
            throw new IllegalArgumentException("Invalid Application Status.");
        }

        for (CandidateApplicationStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        // Throwing IllegalArgumentException allows Spring to see the root cause clearly
        throw new IllegalArgumentException("Invalid Application Status.");
    }

}
