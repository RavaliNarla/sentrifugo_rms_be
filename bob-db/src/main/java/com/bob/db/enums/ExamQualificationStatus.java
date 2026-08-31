package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ExamQualificationStatus {
    NOT_MARKED,
    QUALIFIED,
    QUALIFIED_UNDER_UR,
    DISQUALIFIED;

    @JsonCreator
    public static ExamQualificationStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return  NOT_MARKED;
        }

        for (ExamQualificationStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid Zonal Verification Status.");
    }
}
