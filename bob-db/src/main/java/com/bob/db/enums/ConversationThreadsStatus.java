package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ConversationThreadsStatus {
    PENDING,
    REJECTED,
    PROGRESS,
    L1_PENDING,
    L2_PENDING,
    APPROVED,
    L1_REJECTED,
    L2_REJECTED;
    //L1_APPROVED;

    @JsonCreator
    public static ConversationThreadsStatus fromValue(String value) {
        if (value == null || value.isBlank()) return PENDING;

        for (ConversationThreadsStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        // Throwing IllegalArgumentException allows Spring to see the root cause clearly
        throw new IllegalArgumentException("Invalid status.");
    }
}
