package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DocumentScreeningStatus{
    PENDING,
    VERIFIED,
    REJECTED;

    @JsonCreator
    public static DocumentScreeningStatus fromValue(String value) {
        if (value == null || value.isBlank()) return PENDING;

        for (DocumentScreeningStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        // Throwing IllegalArgumentException allows Spring to see the root cause clearly
        throw new IllegalArgumentException("Invalid Document screening status.");
    }
}
