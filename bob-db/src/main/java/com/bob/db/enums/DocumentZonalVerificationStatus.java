package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DocumentZonalVerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED;

    @JsonCreator
    public static DocumentZonalVerificationStatus fromValue(String value) {
        if (value == null || value.isBlank()) return PENDING;

        for (DocumentZonalVerificationStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid Document zonal verification status.");
    }
}
