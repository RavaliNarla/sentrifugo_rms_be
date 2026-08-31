package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ZonalVerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED,
    PROVISIONALLY_APPROVED,
    ZONAL_REJECTED,
    ZONAL_ABSENT,
    INTERVIEW_ABSENT;

    @JsonCreator
    public static ZonalVerificationStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }

        for (ZonalVerificationStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid Zonal Verification Status.");
    }
}
