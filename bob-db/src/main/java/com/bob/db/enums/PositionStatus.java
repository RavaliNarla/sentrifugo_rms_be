package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PositionStatus {
    NEW,
    DRAFT,
    ACTIVE,
    INACTIVE,
    CLOSED,
    OPEN,
    REOPEN;
    @JsonCreator
    public static PositionStatus fromValue(String value) {
        if (value == null || value.isBlank()) return DRAFT;

        for (PositionStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        // Throwing IllegalArgumentException allows Spring to see the root cause clearly
        throw new IllegalArgumentException("Invalid Position status.");
    }
}
