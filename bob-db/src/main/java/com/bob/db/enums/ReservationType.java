package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ReservationType {

    HORIZONTAL,
    VERTICAL;

    @JsonCreator
    public static ReservationType fromValue(String value) {
        if (value == null || value.isBlank()) return VERTICAL;

        for (ReservationType status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        // Throwing IllegalArgumentException allows Spring to see the root cause clearly
        throw new IllegalArgumentException("Invalid Requisition status.");
    }
}
