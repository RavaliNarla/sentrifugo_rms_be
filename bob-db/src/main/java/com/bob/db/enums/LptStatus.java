package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum LptStatus {
    PASS,
    FAIL,
    EXTENSION_GRANTED;

    @JsonCreator
    public static LptStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (LptStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid LPT status.");
    }
}
