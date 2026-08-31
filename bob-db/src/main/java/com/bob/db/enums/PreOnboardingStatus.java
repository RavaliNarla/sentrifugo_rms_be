package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PreOnboardingStatus {
    NEW,
    SUBMITTED;

    @JsonCreator
    public static PreOnboardingStatus fromValue(String value) {
        if (value == null || value.isBlank()) return NEW;

        for (PreOnboardingStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        // Throwing IllegalArgumentException allows Spring to see the root cause clearly
        throw new IllegalArgumentException("Invalid Requisition status.");
    }
}
