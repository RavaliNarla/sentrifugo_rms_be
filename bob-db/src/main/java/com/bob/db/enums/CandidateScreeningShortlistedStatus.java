package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CandidateScreeningShortlistedStatus {
    DEFAULT,
    YES,
    NO;

    @JsonCreator
    public static CandidateScreeningShortlistedStatus fromValue(String value) {
        if (value == null || value.isBlank()) return DEFAULT;

        for (CandidateScreeningShortlistedStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        // Throwing IllegalArgumentException allows Spring to see the root cause clearly
        throw new IllegalArgumentException("Invalid screening Shortlist status.");
    }
}
