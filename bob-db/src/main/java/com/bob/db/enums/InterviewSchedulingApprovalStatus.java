package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum InterviewSchedulingApprovalStatus {
    PENDING,
    L1_PENDING,
    APPROVED,
    REJECTED;

    @JsonCreator
    public static InterviewSchedulingApprovalStatus fromValue(String value) {
        if (value == null || value.isBlank()) return PENDING;

        for (InterviewSchedulingApprovalStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid scheduling status: " + value);
    }
}