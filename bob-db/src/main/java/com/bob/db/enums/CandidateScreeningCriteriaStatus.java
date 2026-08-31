package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CandidateScreeningCriteriaStatus {
        DEFAULT,
        YES,
        NO,
        DISCREPANCY;


    @JsonCreator
    public static CandidateScreeningCriteriaStatus fromValue(String value) {
        if (value == null || value.isBlank()) return DEFAULT;

        for (CandidateScreeningCriteriaStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        // Throwing IllegalArgumentException allows Spring to see the root cause clearly
        throw new IllegalArgumentException("Invalid screening criteria status.");
    }

}
