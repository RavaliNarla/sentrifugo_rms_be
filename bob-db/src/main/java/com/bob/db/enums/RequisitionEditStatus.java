package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RequisitionEditStatus {
    DRAFT,
    L1_PENDING,
//    L1_APPROVED,
    L1_REJECTED,
    L2_REJECTED,
    APPROVED,
    PUBLISHED,
    CANCELLED,
    L2_PENDING;

    @JsonCreator
    public static RequisitionEditStatus fromValue(String value) {
        if (value == null || value.isBlank()) return DRAFT;
        for (RequisitionEditStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid requisition edit status.");
    }
}
