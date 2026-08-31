package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum WrittenExamConfigurationStatus {
    REJECTED,
    PENDING,
    L1_PENDING,
//    L1_APPROVED,
    L1_REJECTED,
    APPROVED,
    L2_REJECTED,
    FINALIZED,
    L2_PENDING;

    @JsonCreator
    public static WrittenExamConfigurationStatus fromValue(String value) {
        if (value == null || value.isBlank()) return L1_PENDING;
        for (WrittenExamConfigurationStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) return status;
        }
        throw new IllegalArgumentException("Invalid WrittenExamConfiguration status.");
    }
}
