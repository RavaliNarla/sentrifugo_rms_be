package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ApplicationPaymentStatus {
    PENDING,
    FAILED,
    SUCCESS;

    @JsonCreator
    public static ApplicationPaymentStatus fromValue(String value) {
        if (value == null || value.isBlank()){
            throw new IllegalArgumentException("Invalid Application Payment Status.");
        }

        for (ApplicationPaymentStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        // Throwing IllegalArgumentException allows Spring to see the root cause clearly
        throw new IllegalArgumentException("Invalid Application Payment Status.");
    }
}
